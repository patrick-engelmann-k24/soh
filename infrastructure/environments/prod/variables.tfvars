terraform_role_arn         = "arn:aws:iam::556221214774:role/K24.Terraform"
source_repo_branch         = "master"
db_instance_class          = "db.t3.medium"
domain_name                = "prod.kfzteile24.io"
environment                = "prod"
stage                      = "production"
ecp_new_order_sns          = "arn:aws:sns:eu-central-1:726569450381:production-order-export"
ecp_new_order_sns_v3       = "arn:aws:sns:eu-central-1:726569450381:production-order-export-v3"
invoices_from_core_sns     = "arn:aws:sns:eu-central-1:433833759926:production-k24-invoices-integrate-invoices-from-core"
