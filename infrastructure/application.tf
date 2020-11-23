locals {
  service_prefix               = "soh"
  service                      = "soh-bpmn-engine"
  source_repo_name             = "soh-business-processing-engine"
}

module "application_module" {
  source                       = "git@github.com:kfzteile24/bop-infrastructure-application-terraform-module.git"
  service                      = local.service
  codacy_token                 = ""
  source_repo_name             = local.source_repo_name
  source_repo_branch           = var.source_repo_branch
  application_source_code_path = "./"
  runtime                      = "angular"
  health_check_path            = "/healthCheck"
  build_args                   = "--build-arg github_bop_username=${data.aws_ssm_parameter.github_bop_username.value} --build-arg github_bop_token=${data.aws_ssm_parameter.github_bop_token.value}"
  container_cpu                = 2048
  container_memory             = 4096
  container_port               = "8080"
  environment                  = var.environment
  codebuild_vpc                = true

  environment_variables = {
    SPRING_PROFILES_ACTIVE     = "default,${var.stage}"
    name                       = "backend-${var.stage}"
    java_opts                  = "-Xms3072m -Xmx3072m"

    soh_db_host                = module.aurora.this_rds_cluster_endpoint
    soh_db_port                = module.aurora.this_rds_cluster_port
    soh_db_database            = "sales_order_hub"
    soh_order_created          = data.aws_sns_topic.sns_soh_order_created_topic.arn
    soh_order_completed        = data.aws_sns_topic.sns_soh_order_completed_topic.arn
    soh_order_item_cancelled   = data.aws_sns_topic.sns_soh_order_item_cancelled_topic.arn
    soh_order_cancelled        = data.aws_sns_topic.sns_soh_order_cancelled_topic.arn
    soh_invoice_address_changed = data.aws_sns_topic.sns_soh_invoice_address_changed_topic.arn
    soh_delivery_address_changed = data.aws_sns_topic.sns_soh_delivery_address_changed_topic.arn
    soh_sqs_ecp_shop_orders     = aws_sqs_queue.ecp_shop_orders.id
  }

  ssm_secrets_count = 7

  ssm_secrets = {

    newrelic_license           = data.aws_ssm_parameter.new_relic_key.arn
    github_bop_username        = data.aws_ssm_parameter.github_bop_username.arn
    github_bop_token           = data.aws_ssm_parameter.github_bop_token.arn

    soh_db_username            = module.aurora.this_rds_cluster_master_username_arn
    soh_db_password            = module.aurora.this_rds_cluster_master_password_arn

    soh_camunda_username       = data.aws_ssm_parameter.camunda_user.arn
    soh_camunda_password       = data.aws_ssm_parameter.camunda_password.arn
  }

  github_token = var.github_token
}


data "aws_ssm_parameter" "new_relic_key" {
  name = "/new_relic/key"
}

data "aws_ssm_parameter" "camunda_user" {
  name = "/soh-business-processing-engine/camunda/user"
}

data "aws_ssm_parameter" "camunda_password" {
  name = "/soh-business-processing-engine/camunda/password"
}

