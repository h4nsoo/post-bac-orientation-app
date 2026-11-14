output "aws_account_id" {
  description = "Active AWS Account ID"
  value       = data.aws_caller_identity.current.account_id
}

output "aws_region" {
  description = "Deployed AWS Region"
  value       = data.aws_region.current.name
}

output "api_base_url" {
  description = "Base Invoke URL for the API Gateway HTTP v2 stage"
  value       = trimsuffix(aws_apigatewayv2_stage.default.invoke_url, "/")
}

output "recommendations_endpoint" {
  description = "POST endpoint for AI degree recommendations"
  value       = "${trimsuffix(aws_apigatewayv2_stage.default.invoke_url, "/")}/v1/recommendations"
}

output "catalog_endpoint" {
  description = "GET endpoint for dynamic university program catalog delta-sync"
  value       = "${trimsuffix(aws_apigatewayv2_stage.default.invoke_url, "/")}/v1/catalog"
}

output "lambda_function_name" {
  description = "Deployed Kotlin AWS Lambda Function Name"
  value       = aws_lambda_function.api.function_name
}

output "dynamodb_cache_table" {
  description = "DynamoDB Prompt Cache Table Name"
  value       = aws_dynamodb_table.prompt_cache.name
}

output "dynamodb_telemetry_table" {
  description = "DynamoDB Program Telemetry Table Name"
  value       = aws_dynamodb_table.program_telemetry.name
}

output "s3_catalog_bucket" {
  description = "S3 Bucket hosting programs_by_section.json"
  value       = aws_s3_bucket.catalog.bucket
}

output "ssm_parameter_name" {
  description = "SSM Parameter Store path holding the encrypted Gemini API Key"
  value       = aws_ssm_parameter.gemini_api_key.name
}
