data "aws_ssm_parameter" "github_bop_username" {
  name = "/github/k24-boe-deployment/username"
}

data "aws_ssm_parameter" "github_bop_token" {
  name = "/github/k24-boe-deployment/circle-ci-package-generation-token"
}

data "aws_ssm_parameter" "github_token" {
  name = "github-token"
}
