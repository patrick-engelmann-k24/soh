terraform_role_arn             = "arn:aws:iam::967623133951:role/K24.Terraform"
source_repo_branch             = "develop"
db_instance_class              = "db.t3.medium"
domain_name                    = "dev.kfzteile24.io"
environment                    = "dev"
stage                          = "develop"
ecp_new_order_sns              = "arn:aws:sns:eu-central-1:726569450381:development-order-export"
ecp_new_order_sns_v3           = "arn:aws:sns:eu-central-1:726569450381:development-order-export-v3"
d365_order_payment_secured_sns = "arn:aws:sns:eu-central-1:726569450381:qa-1-order-payment-secured-v1"
invoices_from_core_sns         = "arn:aws:sns:eu-central-1:726569450381:integration-1-k24-invoices-integrate-invoices-from-core"
paypal_refund_success_sns      = "arn:aws:sns:eu-central-1:726569450381:qa-1-d365-payment-paypal-refund-instruction-successful-v1"
db_instance_scale              = 1
container_min_count            = 1
container_max_count            = 1
db_performance_insight         = false
ignore_core_sales_invoice      = false
ignore_core_credit_note        = false
ignore_migration_core_sales_invoice      = false
ignore_migration_core_sales_credit_note  = false
ignore_migration_core_sales_order        = false
ignore_sales_order_splitter              = false
pricing_service_endpoint_url    = "https://s8vlbhblll.execute-api.eu-central-1.amazonaws.com/stage"
ignore_set_dissolvement = true
prevent_set_processing = false
bucket_name                    = "integration-1-k24-invoices"
new_relic_application_logging_enabled = true