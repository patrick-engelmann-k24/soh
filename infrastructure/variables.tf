variable "terraform_role_arn" {
  type = string
}

variable "source_repo_branch" {
  type = string
}

variable "db_instance_class" {
  type = string
}

variable "db_instance_scale" {
  type = number
}

variable "container_min_count" {
  type = number
}

variable "container_max_count" {
  type = number
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

variable "ecp_new_order_sns" {
  type = string
}

variable "ecp_new_order_sns_v3" {
  type = string
}

variable "invoices_from_core_sns" {
  type = string
}

variable "db_performance_insight" {
  type = bool
}