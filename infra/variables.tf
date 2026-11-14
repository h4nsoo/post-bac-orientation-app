variable "aws_region" {
  description = "AWS region for deploying all serverless resources"
  type        = string
  default     = "eu-central-1"
}

variable "project_name" {
  description = "Project prefix for all AWS resources"
  type        = string
  default     = "unicompass"
}

variable "gemini_api_key" {
  description = "Google Gemini API Key stored in SSM Parameter Store (SecureString)"
  type        = string
  sensitive   = true
}

variable "lambda_jar_path" {
  description = "Optional path to compiled Kotlin ShadowJar. Uses bootstrap archive when empty."
  type        = string
  default     = ""
}
