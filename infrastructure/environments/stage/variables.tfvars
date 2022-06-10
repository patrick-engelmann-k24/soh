terraform_role_arn             = "arn:aws:iam::307487914182:role/K24.Terraform"
source_repo_branch             = "develop"
db_instance_class              = "db.r4.2xlarge"
domain_name                    = "stage.kfzteile24.io"
environment                    = "stage"
stage                          = "staging-1"
ecp_new_order_sns              = "arn:aws:sns:eu-central-1:433833759926:staging-order-export"
ecp_new_order_sns_v3           = "arn:aws:sns:eu-central-1:433833759926:staging-order-export-v3"
d365_order_payment_secured_sns = "arn:aws:sns:eu-central-1:433833759926:staging-order-payment-secured-v1"
invoices_from_core_sns         = "arn:aws:sns:eu-central-1:433833759926:staging-k24-invoices-integrate-invoices-from-core"
db_instance_scale              = 1
container_min_count            = 1
container_max_count            = 3
db_performance_insight         = true
ignore_core_sales_invoice      = false
ignore_core_credit_note        = false
ignore_migration_core_sales_invoice      = false
ignore_migration_core_sales_credit_note  = false
ignore_migration_core_sales_order        = false
ignore_sales_order_splitter              = true
pricing_service_endpoint_url   = "https://s8vlbhblll.execute-api.eu-central-1.amazonaws.com/stage"