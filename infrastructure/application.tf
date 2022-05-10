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
  min_capacity                 = var.container_min_count
  max_capacity                 = var.container_max_count
  use_stickiness               = true

  environment_variables = {
    SPRING_PROFILES_ACTIVE                     = "default,${var.stage}"
    name                                       = "backend-${var.stage}"
    java_opts                                  = "-Xms3072m -Xmx3072m"

    soh_db_host                                = module.aurora.this_rds_cluster_endpoint
    soh_db_port                                = module.aurora.this_rds_cluster_port
    soh_db_database                            = "sales_order_hub"

    soh_order_created_v2                       = data.aws_sns_topic.sns_soh_order_created_v2_topic.arn
    soh_order_completed                        = data.aws_sns_topic.sns_soh_sales_order_completed_topic_v1.arn
    soh_order_item_cancelled                   = data.aws_sns_topic.sns_soh_order_item_cancelled_topic.arn
    soh_order_rows_cancelled                   = data.aws_sns_topic.sns_soh_order_rows_cancelled_topic_v1.arn
    soh_order_cancelled                        = data.aws_sns_topic.sns_soh_order_cancelled_topic.arn
    soh_invoice_address_changed                = data.aws_sns_topic.sns_soh_invoice_address_changed_topic.arn
    soh_delivery_address_changed               = data.aws_sns_topic.sns_soh_delivery_address_changed_topic.arn
    soh_sales_order_row_cancellation           = data.aws_sns_topic.sns_soh_sales_order_row_cancellation_v1.arn
    soh_sales_order_cancellation               = data.aws_sns_topic.sns_soh_sales_order_cancellation_v1.arn
    soh_order_invoice_created_v1               = data.aws_sns_topic.sns_soh_order_invoice_created_v1.arn
    sns_soh_shipment_confirmed_v1              = data.aws_sns_topic.sns_soh_shipment_confirmed_v1.arn
    sns_soh_return_order_created_v1            = data.aws_sns_topic.sns_soh_return_order_created_v1.arn
    sns_soh_core_invoice_received_v1           = data.aws_sns_topic.sns_soh_core_invoice_received_v1.arn
    sns_soh_credit_note_received_v1            = data.aws_sns_topic.sns_soh_credit_note_received_v1.arn
    sns_migration_soh_order_created_v2         = data.aws_sns_topic.sns_migration_soh_order_created_v2.arn
    sns_migration_soh_sales_order_row_cancelled_v1 = data.aws_sns_topic.sns_migration_soh_sales_order_row_cancelled_v1.arn
    sns_migration_soh_sales_order_cancelled_v1 = data.aws_sns_topic.sns_migration_soh_sales_order_cancelled_v1.arn

    soh_sqs_ecp_shop_orders                    = aws_sqs_queue.ecp_shop_orders.id
    soh_sqs_bc_shop_orders                     = aws_sqs_queue.bc_shop_orders.id
    soh_sqs_core_shop_orders                   = aws_sqs_queue.core_shop_orders.id
    soh_sqs_order_item_shipped                 = aws_sqs_queue.soh_order_item_shipped.id
    soh_sqs_order_payment_secured              = aws_sqs_queue.soh_order_payment_secured.id
    soh_sqs_order_item_transmitted_to_logistic = aws_sqs_queue.soh_order_item_transmitted_to_logistic.id
    soh_sqs_order_item_packing_started         = aws_sqs_queue.soh_order_item_packing_started.id
    soh_sqs_order_item_tracking_id_received    = aws_sqs_queue.soh_order_item_tracking_id_received.id
    soh_sqs_order_item_tour_started            = aws_sqs_queue.soh_order_item_tour_started.id
    soh_sqs_invoices_from_core                 = aws_sqs_queue.soh_invoices_from_core.id
    soh_sqs_core_cancellation                  = aws_sqs_queue.soh_core_cancellation.id
    soh_sqs_subsequent_delivery_received       = aws_sqs_queue.soh_subsequent_delivery_received.id
    soh_sqs_d365_order_payment_secured         = aws_sqs_queue.d365_order_payment_secured.id
    soh_sqs_dropshipment_shipment_confirmed    = aws_sqs_queue.soh_dropshipment_shipment_confirmed.id
    soh_sqs_dropshipment_purchase_order_booked = aws_sqs_queue.dropshipment_purchase_order_booked.id
    soh_sqs_core_sales_credit_note_created     = aws_sqs_queue.soh_core_sales_credit_note_created.id
    soh_sqs_core_sales_invoice_created         = aws_sqs_queue.soh_core_sales_invoice_created.id
    soh_sqs_migration_core_sales_order_created = aws_sqs_queue.soh_migration_core_sales_order_created.id
    soh_sqs_migration_core_sales_invoice_created = aws_sqs_queue.soh_migration_core_sales_invoice_created.id

    ignore_core_sales_invoice                  = var.ignore_core_sales_invoice
    ignore_core_credit_note                    = var.ignore_core_credit_note
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

