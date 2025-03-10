data "aws_iam_policy_document" "sns_sqs_access_policy" {
  statement {
    effect = "Allow"

    actions = [
      "sns:*",
    ]

    resources = [
      data.aws_sns_topic.sns_soh_order_created_v2_topic.arn,
      data.aws_sns_topic.sns_soh_sales_order_completed_topic_v1.arn,
      data.aws_sns_topic.sns_soh_invoice_address_changed_topic.arn,
      data.aws_sns_topic.sns_soh_sales_order_row_cancellation_v1.arn,
      data.aws_sns_topic.sns_soh_sales_order_cancellation_v1.arn,
      data.aws_sns_topic.sns_soh_order_invoice_created_v1.arn,
      data.aws_sns_topic.sns_soh_shipment_confirmed_v1.arn,
      data.aws_sns_topic.sns_soh_dropshipment_purchase_order_booked_v1.arn,
      data.aws_sns_topic.sns_soh_return_order_created_v1.arn,
      data.aws_sns_topic.sns_core_sales_credit_note_created_v1.arn,
      data.aws_sns_topic.sns_soh_core_invoice_received_v1.arn,
      data.aws_sns_topic.sns_soh_credit_note_received_v1.arn,
      data.aws_sns_topic.sns_soh_credit_note_created_v1.arn,
      data.aws_sns_topic.sns_soh_credit_note_document_generated_v1.arn,
      data.aws_sns_topic.sns_migration_soh_order_created_v2.arn,
      data.aws_sns_topic.sns_migration_soh_sales_order_row_cancelled_v1.arn,
      data.aws_sns_topic.sns_migration_soh_sales_order_cancelled_v1.arn,
      data.aws_sns_topic.sns_migration_soh_return_order_created_v1.arn,
      data.aws_sns_topic.sns_migration_core_sales_invoice_created.arn,
      data.aws_sns_topic.sns_migration_core_sales_credit_note_created.arn,
      data.aws_sns_topic.sns_soh_dropshipment_order_created_v1.arn,
      data.aws_sns_topic.sns_soh_dropshipment_order_return_notified_v1.arn,
      data.aws_sns_topic.sns_parcel_shipped.arn,
      data.aws_sns_topic.sns_soh_payout_receipt_confirmation_received_v1.arn,
      data.aws_sns_topic.sns_soh_invoice_pdf_generation_triggered_v1.arn,
      data.aws_sns_topic.sns_core_sales_order_cancelled_v1.arn
    ]
  }
  statement {
    effect = "Allow"

    actions = [
      "sqs:*",
    ]

    resources = [
      aws_sqs_queue.ecp_shop_orders.arn,
      aws_sqs_queue.bc_shop_orders.arn,
      aws_sqs_queue.core_shop_orders.arn,
      aws_sqs_queue.soh_invoices_from_core.arn,
      aws_sqs_queue.d365_order_payment_secured.arn,
      aws_sqs_queue.soh_dropshipment_shipment_confirmed.arn,
      aws_sqs_queue.soh_dropshipment_purchase_order_booked.arn,
      aws_sqs_queue.soh_dropshipment_purchase_order_return_confirmed.arn,
      aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.arn,
      aws_sqs_queue.soh_core_sales_credit_note_created.arn,
      aws_sqs_queue.soh_core_sales_invoice_created.arn,
      aws_sqs_queue.soh_migration_core_sales_order_created.arn,
      aws_sqs_queue.soh_migration_core_sales_invoice_created.arn,
      aws_sqs_queue.soh_migration_core_sales_credit_note_created.arn,
      aws_sqs_queue.soh_parcel_shipped.arn,
      aws_sqs_queue.soh_paypal_refund_instruction_successful.arn,
      aws_sqs_queue.soh_core_sales_order_cancelled.arn,

      aws_sqs_queue.ecp_shop_orders_dlq.arn,
      aws_sqs_queue.bc_shop_orders_dlq.arn,
      aws_sqs_queue.core_shop_orders_dlq.arn,
      aws_sqs_queue.soh_core_sales_invoice_created_dlq.arn,
      aws_sqs_queue.soh_parcel_shipped_dlq.arn,
      aws_sqs_queue.soh_migration_core_sales_order_created_dlq.arn,
      aws_sqs_queue.soh_migration_core_sales_invoice_created_dlq.arn,
      aws_sqs_queue.soh_migration_core_sales_credit_note_created_dlq.arn,
      aws_sqs_queue.soh_invoices_from_core_dlq.arn,
      aws_sqs_queue.soh_core_sales_credit_note_created_dlq.arn,
      aws_sqs_queue.soh_paypal_refund_instruction_successful_dlq.arn,
      aws_sqs_queue.d365_order_payment_secured_dlq.arn,
      aws_sqs_queue.soh_paypal_refund_instruction_successful_dlq.arn,
      aws_sqs_queue.soh_dropshipment_purchase_order_booked_dlq.arn,
      aws_sqs_queue.soh_dropshipment_shipment_confirmed_dlq.arn,
      aws_sqs_queue.soh_dropshipment_purchase_order_return_notified_dlq.arn,
      aws_sqs_queue.soh_dropshipment_purchase_order_return_confirmed_dlq.arn,
      aws_sqs_queue.soh_core_sales_order_cancelled_dlq.arn
    ]
  }
  statement {
    effect = "Allow"

    actions = [
      "s3:*",
    ]

    resources = [
      data.aws_s3_bucket.ecp_invoice_bucket.arn,
      "${data.aws_s3_bucket.ecp_invoice_bucket.arn}/*"
    ]

  }

  statement {
    effect = "Allow"

    actions = [
      "ses:*"
    ]

    resources = ["*"]
  }

}

resource "aws_iam_role_policy" "task_role_policy" {
  name   = "${local.service}-task-role-policy"
  policy = data.aws_iam_policy_document.sns_sqs_access_policy.json
  role   = module.application_module.task_role_name
}

resource "aws_iam_user" "sales_order_hub_technical" {
  name = "${local.service}-technical-user"

  tags = {
    Name    = "soh business-processing-engine technical user"
    Group   = local.service
    GitRepo = "soh-business-processing-engine"
  }
}

resource "aws_iam_access_key" "sales_order_hub_technical" {
  user = aws_iam_user.sales_order_hub_technical.id
}

resource "aws_ssm_parameter" "sales_order_hub_access_key_id" {
  name  = "/${local.service}/sales_order_hub_bucket/access_key_id"
  type  = "SecureString"
  value = aws_iam_access_key.sales_order_hub_technical.id
}

resource "aws_ssm_parameter" "sales_order_hub_secret_access_key" {
  name  = "/${local.service}/sales_order_hub_bucket/secret_access_key"
  type  = "SecureString"
  value = aws_iam_access_key.sales_order_hub_technical.secret
}