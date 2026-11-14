# ==============================================================================
# DynamoDB Tables (Serverless On-Demand — $0.00 Idle Cost)
# ==============================================================================

# 1. Semantic/Exact LLM Response Cache with automatic 48h TTL expiration
resource "aws_dynamodb_table" "prompt_cache" {
  name         = "${var.project_name}-prompt-cache"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "prompt_hash"

  attribute {
    name = "prompt_hash"
    type = "S"
  }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }
}

# 2. Live National Orientation Demand Telemetry (Atomic Counter per Program Code)
resource "aws_dynamodb_table" "program_telemetry" {
  name         = "${var.project_name}-program-telemetry"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "program_code"

  attribute {
    name = "program_code"
    type = "S"
  }
}
