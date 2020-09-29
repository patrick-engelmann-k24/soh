bucket = "k24-bop-terraform-state-stage"

key = "sales-order-hub/terraform.tfstate"

region = "eu-central-1"

role_arn = "arn:aws:iam::307487914182:role/K24.Terraform"

encrypt = "true"

dynamodb_table = "terraform-lock"