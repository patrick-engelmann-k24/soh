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

resource "aws_ssm_parameter" "pricing_service_api_key" {
  name      = "/${local.service}/pricing_service_api_key"
  type      = "SecureString"
  overwrite = true
  value     = data.sops_file.secrets.data["pricing_service_api_key"]

  tags = {
    Name        = "${local.service} pricing service api key"
    Group       = local.service
    GitRepoName = local.service
  }
}

resource "aws_ssm_parameter" "soh_source_email" {
  name      = "/${local.service}/soh_source_email"
  type      = "SecureString"
  overwrite = true
  value     = data.sops_file.secrets.data["soh_source_email"]

  tags = {
    Name        = "${local.service} soh source email"
    Group       = local.service
    GitRepoName = local.service
  }
}

resource "aws_ssm_parameter" "finance_destination_email" {
  name      = "/${local.service}/finance_destination_email"
  type      = "SecureString"
  overwrite = true
  value     = data.sops_file.secrets.data["finance_destination_email"]

  tags = {
    Name        = "${local.service} finance destination email"
    Group       = local.service
    GitRepoName = local.service
  }
}

resource "aws_ssm_parameter" "refund_cc_email" {
  name      = "/${local.service}/refund_cc_email"
  type      = "SecureString"
  overwrite = true
  value     = data.sops_file.secrets.data["refund_cc_email"]

  tags = {
    Name        = "${local.service} refund cc email"
    Group       = local.service
    GitRepoName = local.service
  }
}

resource "aws_ssm_parameter" "soh_source_password" {
  name      = "/${local.service}/soh_source_password"
  type      = "SecureString"
  overwrite = true
  value     = data.sops_file.secrets.data["soh_source_password"]

  tags = {
    Name        = "${local.service} soh source passwrod"
    Group       = local.service
    GitRepoName = local.service
  }
}