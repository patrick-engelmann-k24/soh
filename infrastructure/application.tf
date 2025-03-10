locals {
  service_prefix               = "soh"
  service                      = "soh-bpmn-engine"
  source_repo_name             = "soh-business-processing-engine"
}

module "application_module" {
  source                       = "git@github.com:kfzteile24/bop-infrastructure-application-terraform-module.git?ref=v4.0.0"
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
    environment                                = var.environment

    soh_db_host                                = module.aurora.this_rds_cluster_endpoint
    soh_db_port                                = module.aurora.this_rds_cluster_port
    soh_db_database                            = "sales_order_hub"

    soh_order_created_v2                       = data.aws_sns_topic.sns_soh_order_created_v2_topic.arn
    soh_order_completed                        = data.aws_sns_topic.sns_soh_sales_order_completed_topic_v1.arn
    soh_invoice_address_changed                = data.aws_sns_topic.sns_soh_invoice_address_changed_topic.arn
    soh_sales_order_row_cancellation           = data.aws_sns_topic.sns_soh_sales_order_row_cancellation_v1.arn
    soh_sales_order_cancellation               = data.aws_sns_topic.sns_soh_sales_order_cancellation_v1.arn
    soh_order_invoice_created_v1               = data.aws_sns_topic.sns_soh_order_invoice_created_v1.arn
    sns_soh_shipment_confirmed_v1              = data.aws_sns_topic.sns_soh_shipment_confirmed_v1.arn
    sns_soh_return_order_created_v1            = data.aws_sns_topic.sns_soh_return_order_created_v1.arn
    sns_soh_core_invoice_received_v1           = data.aws_sns_topic.sns_soh_core_invoice_received_v1.arn
    sns_soh_credit_note_received_v1            = data.aws_sns_topic.sns_soh_credit_note_received_v1.arn
    sns_soh_credit_note_created_v1             = data.aws_sns_topic.sns_soh_credit_note_created_v1.arn
    sns_soh_credit_note_document_generated_v1  = data.aws_sns_topic.sns_soh_credit_note_document_generated_v1.arn
    sns_migration_soh_order_created_v2         = data.aws_sns_topic.sns_migration_soh_order_created_v2.arn
    sns_migration_soh_sales_order_row_cancelled_v1 = data.aws_sns_topic.sns_migration_soh_sales_order_row_cancelled_v1.arn
    sns_migration_soh_sales_order_cancelled_v1 = data.aws_sns_topic.sns_migration_soh_sales_order_cancelled_v1.arn
    sns_migration_soh_return_order_created_v1  = data.aws_sns_topic.sns_migration_soh_return_order_created_v1.arn
    sns_soh_dropshipment_order_created_v1      = data.aws_sns_topic.sns_soh_dropshipment_order_created_v1.arn
    sns_soh_dropshipment_order_return_notified_v1 = data.aws_sns_topic.sns_soh_dropshipment_order_return_notified_v1.arn
    sns_soh_payout_receipt_confirmation_received_v1 = data.aws_sns_topic.sns_soh_payout_receipt_confirmation_received_v1.arn
    sns_soh_invoice_pdf_generation_triggered_v1 = data.aws_sns_topic.sns_soh_invoice_pdf_generation_triggered_v1.arn

    soh_sqs_ecp_shop_orders                    = aws_sqs_queue.ecp_shop_orders.id
    soh_sqs_bc_shop_orders                     = aws_sqs_queue.bc_shop_orders.id
    soh_sqs_core_shop_orders                   = aws_sqs_queue.core_shop_orders.id
    soh_sqs_invoices_from_core                 = aws_sqs_queue.soh_invoices_from_core.id
    soh_sqs_d365_order_payment_secured         = aws_sqs_queue.d365_order_payment_secured.id
    soh_sqs_dropshipment_shipment_confirmed    = aws_sqs_queue.soh_dropshipment_shipment_confirmed.id
    soh_sqs_dropshipment_purchase_order_booked = aws_sqs_queue.soh_dropshipment_purchase_order_booked.id
    soh_sqs_dropshipment_purchase_order_return_confirmed = aws_sqs_queue.soh_dropshipment_purchase_order_return_confirmed.id
    soh_sqs_dropshipment_purchase_order_return_notified = aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.id
    soh_sqs_core_sales_credit_note_created     = aws_sqs_queue.soh_core_sales_credit_note_created.id
    soh_sqs_core_sales_invoice_created         = aws_sqs_queue.soh_core_sales_invoice_created.id
    soh_sqs_migration_core_sales_order_created = aws_sqs_queue.soh_migration_core_sales_order_created.id
    soh_sqs_migration_core_sales_invoice_created = aws_sqs_queue.soh_migration_core_sales_invoice_created.id
    soh_sqs_migration_core_sales_credit_note_created = aws_sqs_queue.soh_migration_core_sales_credit_note_created.id
    soh_sqs_parcel_shipped                     = aws_sqs_queue.soh_parcel_shipped.id
    soh_sqs_paypal_refund_instruction_successful = aws_sqs_queue.soh_paypal_refund_instruction_successful.id
    soh_sqs_core_sales_order_cancelled         = aws_sqs_queue.soh_core_sales_order_cancelled.id

    ignore_core_sales_invoice                  = var.ignore_core_sales_invoice
    ignore_core_credit_note                    = var.ignore_core_credit_note
    ignore_migration_core_sales_invoice        = var.ignore_migration_core_sales_invoice
    ignore_migration_core_sales_credit_note    = var.ignore_migration_core_sales_credit_note
    ignore_migration_core_sales_order          = var.ignore_migration_core_sales_order
    ignore_sales_order_splitter                = var.ignore_sales_order_splitter
    soh_bpmn_pricing_service_url               = var.pricing_service_endpoint_url
    soh_bpmn_http_connection_timeout_seconds   = 8
    ignore_set_dissolvement                    = var.ignore_set_dissolvement
    prevent_set_processing                     = var.prevent_set_processing
  }

  ssm_secrets_count = 14

  ssm_secrets = {

    newrelic_license           = data.aws_ssm_parameter.new_relic_key.arn
    github_bop_username        = data.aws_ssm_parameter.github_bop_username.arn
    github_bop_token           = data.aws_ssm_parameter.github_bop_token.arn

    soh_db_username            = module.aurora.this_rds_cluster_master_username_arn
    soh_db_password            = module.aurora.this_rds_cluster_master_password_arn

    soh_camunda_username       = data.aws_ssm_parameter.camunda_user.arn
    soh_camunda_password       = data.aws_ssm_parameter.camunda_password.arn

    pdh_client_id              = aws_ssm_parameter.pdh_client_id.arn
    pdh_client_secret          = aws_ssm_parameter.pdh_client_secret.arn

    pricing_service_api_key    = aws_ssm_parameter.pricing_service_api_key.arn

    soh_source_email           = aws_ssm_parameter.soh_source_email.arn
    finance_destination_email  = aws_ssm_parameter.finance_destination_email.arn
    refund_cc_email            = aws_ssm_parameter.refund_cc_email.arn
    soh_source_password        = aws_ssm_parameter.soh_source_password.arn
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

