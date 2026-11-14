# ==============================================================================
# AWS Lambda (Java 21 / Kotlin Runtime on Graviton2 arm64 + JVM SnapStart)
# ==============================================================================

# Minimal bootstrap archive so Terraform can provision the Lambda before the
# Kotlin ShadowJar is compiled. Once `:cloud-lambda:shadowJar` is built, pass
# `-var="lambda_jar_path=..."` or copy the JAR to replace the package.
data "archive_file" "bootstrap" {
  type        = "zip"
  output_path = "${path.module}/.bootstrap/bootstrap.zip"

  source {
    content  = "Manifest-Version: 1.0\nCreated-By: UniCompass-Terraform-Bootstrap\n"
    filename = "META-INF/MANIFEST.MF"
  }
}

locals {
  effective_package_path = var.lambda_jar_path != "" ? var.lambda_jar_path : data.archive_file.bootstrap.output_path
  effective_package_hash = var.lambda_jar_path != "" ? filebase64sha256(var.lambda_jar_path) : data.archive_file.bootstrap.output_base64sha256
}

resource "aws_cloudwatch_log_group" "lambda_logs" {
  name              = "/aws/lambda/${var.project_name}-api"
  retention_in_days = 3
}

resource "aws_iam_role" "lambda_exec" {
  name = "${var.project_name}-lambda-exec-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "lambda.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "lambda_least_privilege" {
  name = "${var.project_name}-lambda-least-privilege"
  role = aws_iam_role.lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "${aws_cloudwatch_log_group.lambda_logs.arn}:*"
      },
      {
        Sid    = "DynamoDBCacheAndTelemetry"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:BatchGetItem",
          "dynamodb:Query",
          "dynamodb:Scan"
        ]
        Resource = [
          aws_dynamodb_table.prompt_cache.arn,
          aws_dynamodb_table.program_telemetry.arn
        ]
      },
      {
        Sid    = "S3CatalogRead"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:HeadObject"
        ]
        Resource = "${aws_s3_bucket.catalog.arn}/*"
      },
      {
        Sid    = "SSMReadGeminiKey"
        Effect = "Allow"
        Action = [
          "ssm:GetParameter"
        ]
        Resource = aws_ssm_parameter.gemini_api_key.arn
      },
      {
        Sid    = "KMSDecryptSSM"
        Effect = "Allow"
        Action = [
          "kms:Decrypt"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_lambda_function" "api" {
  function_name    = "${var.project_name}-api"
  description      = "UniCompass Kotlin Serverless Backend (AI Orchestration, Semantic Cache & Demand Telemetry)"
  role             = aws_iam_role.lambda_exec.arn
  runtime          = "java21"
  architectures    = ["arm64"]
  handler          = "com.example.orientation_app.cloud.OrientationLambdaHandler::handleRequest"
  filename         = local.effective_package_path
  source_code_hash = local.effective_package_hash
  memory_size      = 512
  timeout          = 20

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = {
      PROMPT_CACHE_TABLE   = aws_dynamodb_table.prompt_cache.name
      TELEMETRY_TABLE      = aws_dynamodb_table.program_telemetry.name
      CATALOG_BUCKET       = aws_s3_bucket.catalog.bucket
      CATALOG_OBJECT_KEY   = aws_s3_object.programs_json.key
      SSM_GEMINI_KEY_PARAM = aws_ssm_parameter.gemini_api_key.name
    }
  }

  depends_on = [
    aws_cloudwatch_log_group.lambda_logs,
    aws_iam_role_policy.lambda_least_privilege
  ]
}
