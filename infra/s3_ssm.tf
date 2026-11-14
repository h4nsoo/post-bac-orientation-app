# ==============================================================================
# S3 Bucket for Dynamic University Catalog Sync + SSM Parameter Store for Secrets
# ==============================================================================

resource "aws_s3_bucket" "catalog" {
  bucket        = "${var.project_name}-catalog-${data.aws_caller_identity.current.account_id}"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "catalog" {
  bucket = aws_s3_bucket.catalog.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Upload the official Tunisian Ministry programs dataset to S3 for delta-syncing
resource "aws_s3_object" "programs_json" {
  bucket       = aws_s3_bucket.catalog.id
  key          = "programs_by_section.json"
  source       = "${path.module}/../programs_by_section.json"
  content_type = "application/json"
  etag         = filemd5("${path.module}/../programs_by_section.json")
}

# KMS-encrypted Gemini API Key stored in SSM Parameter Store ($0.00/month)
resource "aws_ssm_parameter" "gemini_api_key" {
  name        = "/${var.project_name}/gemini_api_key"
  description = "Google Gemini API Key for UniCompass AI Recommendation Engine"
  type        = "SecureString"
  value       = var.gemini_api_key
}
