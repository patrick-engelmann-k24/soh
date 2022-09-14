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

variable "d365_order_payment_secured_sns" {
  type = string
}

variable "invoices_from_core_sns" {
  type = string
}

variable "db_performance_insight" {
  type = bool
}

variable "ignore_core_sales_invoice" {
  type = bool
}

variable "ignore_core_credit_note" {
  type = bool
}

variable "ignore_migration_core_sales_invoice" {
  type = bool
}

variable "ignore_migration_core_sales_credit_note" {
  type = bool
}

variable "ignore_migration_core_sales_order" {
  type = bool
}

variable "ignore_sales_order_splitter" {
  type = bool
}

variable "pricing_service_endpoint_url" {
  type = string
}

variable "ignore_set_dissolvement" {
  type = bool
}

variable "prevent_set_processing" {
  type = bool
}

variable "bucket_name" {
  type = string
}
