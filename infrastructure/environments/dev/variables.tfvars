terraform_role_arn         = "arn:aws:iam::967623133951:role/K24.Terraform"
source_repo_branch         = "develop"
db_instance_class          = "db.t3.medium"
domain_name                = "dev.kfzteile24.io"
environment                = "dev"
stage                      = "develop"
ecp_new_order_sns          = "arn:aws:sns:eu-central-1:726569450381:development-order-export"
invoices_from_core_sns      = "arn:aws:sns:eu-central-1:433833759926:staging-k24-invoices-integrate-invoices-from-core"