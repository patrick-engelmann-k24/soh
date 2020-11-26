bucket = "k24-bop-terraform-state-development"

key = "{%service-name%}/terraform.tfstate"

region = "eu-central-1"

role_arn = "arn:aws:iam::967623133951:role/K24.Terraform"

encrypt = "true"

dynamodb_table = "terraform-lock"
