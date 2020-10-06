terraform_role_arn         = "arn:aws:iam::967623133951:role/K24.Terraform"
source_repo_branch         = "develop"
db_instance_class          = "db.t3.medium"
domain_name                = "dev.kfzteile24.io"
environment                = "dev"
stage                      = "develop"
aws_sqs_ecp_orders_allowed_iams = ["arn:aws:iam::967623133951:root","arn:aws:iam::726569450381:root"]
ecp_new_order_sns          = "arn:aws:sns:eu-central-1:726569450381:development-order-export"