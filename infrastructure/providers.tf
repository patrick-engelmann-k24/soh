provider "aws" {
  region  = "eu-central-1"
  version = "=2.70.0"

  assume_role {
    role_arn = var.terraform_role_arn
  }
}

provider "github" {
  token        = data.aws_ssm_parameter.github_token.value
  organization = "kfzteile24"
}
