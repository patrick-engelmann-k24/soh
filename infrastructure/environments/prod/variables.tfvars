terraform_role_arn         = "arn:aws:iam::556221214774:role/K24.Terraform"
source_repo_branch         = "master"
db_instance_class          = "db.t3.large"
domain_name                = "prod.kfzteile24.io"
environment                = "prod"
stage                      = "production"
ecp_new_order_sns          = "arn:aws:sns:eu-central-1:433833759926:production-order-export"
ecp_new_order_sns_v3       = "arn:aws:sns:eu-central-1:433833759926:production-order-export-v3"
invoices_from_core_sns     = "arn:aws:sns:eu-central-1:433833759926:production-k24-invoices-integrate-invoices-from-core"
db_instance_scale          = 1
container_min_count        = 2
container_max_count        = 3
db_performance_insight     = true