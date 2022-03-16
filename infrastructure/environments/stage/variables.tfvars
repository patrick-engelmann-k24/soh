terraform_role_arn             = "arn:aws:iam::307487914182:role/K24.Terraform"
source_repo_branch             = "develop"
db_instance_class              = "db.t3.medium"
domain_name                    = "stage.kfzteile24.io"
environment                    = "stage"
stage                          = "staging-1"
ecp_new_order_sns              = "arn:aws:sns:eu-central-1:433833759926:staging-order-export"
ecp_new_order_sns_v3           = "arn:aws:sns:eu-central-1:433833759926:staging-order-export-v3"
d365_order_payment_secured_sns = "arn:aws:sns:eu-central-1:433833759926:staging-order-payment-secured-v1"
invoices_from_core_sns         = "arn:aws:sns:eu-central-1:433833759926:staging-k24-invoices-integrate-invoices-from-core"
db_instance_scale              = 1
container_min_count            = 1
container_max_count            = 1
db_performance_insight         = false