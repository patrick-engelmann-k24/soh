data "sops_file" "secrets" {
  source_file = "environments/${var.environment}/secrets.enc.yaml"
}

resource "aws_ssm_parameter" "pdh_client_id" {
  name      = "/${local.service}/pdh_client_id"
  type      = "SecureString"
  overwrite = true
  value     = data.sops_file.secrets.data["pdh_client_id"]

  tags = {
    Name        = "${local.service} pdh client id"
    Group       = local.service
    GitRepoName = local.service
  }
}

resource "aws_ssm_parameter" "pdh_client_secret" {
  name      = "/${local.service}/pdh_client_secret"
  type      = "SecureString"
  overwrite = true
  value     = data.sops_file.secrets.data["pdh_client_secret"]

  tags = {
    Name        = "${local.service} pdh client secret"
    Group       = local.service
    GitRepoName = local.service
  }
}
