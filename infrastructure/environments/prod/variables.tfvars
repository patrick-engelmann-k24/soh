terraform_role_arn             = "arn:aws:iam::556221214774:role/K24.Terraform"
source_repo_branch             = "master"
db_instance_class              = "db.r5.8xlarge"
domain_name                    = "prod.kfzteile24.io"
environment                    = "prod"
stage                          = "production"
ecp_new_order_sns              = "arn:aws:sns:eu-central-1:433833759926:production-order-export"
ecp_new_order_sns_v3           = "arn:aws:sns:eu-central-1:433833759926:production-order-export-v3"
d365_order_payment_secured_sns = "arn:aws:sns:eu-central-1:433833759926:production-order-payment-secured-v1"
invoices_from_core_sns         = "arn:aws:sns:eu-central-1:433833759926:production-k24-invoices-integrate-invoices-from-core"
db_instance_scale              = 1
container_min_count            = 4
container_max_count            = 5
db_performance_insight         = true
ignore_core_sales_invoice      = false
ignore_core_credit_note        = false
ignore_migration_core_sales_invoice      = false
ignore_migration_core_sales_credit_note  = false
ignore_migration_core_sales_order        = false
ignore_sales_order_splitter              = false
pricing_service_endpoint_url   = "https://2bqdw5i7yj.execute-api.eu-central-1.amazonaws.com/prod"
ignore_set_dissolvement = false
prevent_set_processing = false