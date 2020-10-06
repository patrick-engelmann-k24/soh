variable "terraform_role_arn" {
  type = string
}

variable "source_repo_branch" {
  type = string
}

variable "db_instance_class" {
  type = string
}

variable "domain_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "stage" {
  type = string
}

variable "github_token" {
  type = string
}

variable "aws_sqs_ecp_orders_allowed_iams" {
  type = list
}

variable "ecp_new_order_sns" {
  type = string
}