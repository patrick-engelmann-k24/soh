bucket = "k24-bop-terraform-state-production"

key = "{%service-name%}/terraform.tfstate"

region = "eu-central-1"

role_arn = "arn:aws:iam::556221214774:role/K24.Terraform"

encrypt = "true"

dynamodb_table = "terraform-lock"
