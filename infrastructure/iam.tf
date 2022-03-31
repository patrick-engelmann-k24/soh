data "aws_iam_policy_document" "sns_sqs_access_policy" {
  statement {
    effect = "Allow"

    actions = [
      "sns:*",
    ]

    resources = [
      data.aws_sns_topic.sns_soh_order_created_v2_topic.arn,
      data.aws_sns_topic.sns_soh_order_completed_topic.arn,
      data.aws_sns_topic.sns_soh_order_cancelled_topic.arn,
      data.aws_sns_topic.sns_soh_order_item_cancelled_topic.arn,
      data.aws_sns_topic.sns_soh_order_rows_cancelled_topic_v1.arn,
      data.aws_sns_topic.sns_soh_invoice_address_changed_topic.arn,
      data.aws_sns_topic.sns_soh_delivery_address_changed_topic.arn,
      data.aws_sns_topic.sns_soh_sales_order_row_cancellation_v1.arn,
      data.aws_sns_topic.sns_soh_sales_order_cancellation_v1.arn,
      data.aws_sns_topic.sns_core_subsequent_delivery_note_printed.arn,
      data.aws_sns_topic.sns_soh_order_invoice_created_v1.arn,
      data.aws_sns_topic.sns_soh_shipment_confirmed_v1.arn,
      data.aws_sns_topic.sns_soh_dropshipment_purchase_order_booked_v1.arn
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
      aws_sqs_queue.soh_order_item_shipped.arn,
      aws_sqs_queue.soh_order_payment_secured.arn,
      aws_sqs_queue.soh_order_item_transmitted_to_logistic.arn,
      aws_sqs_queue.soh_order_item_packing_started.arn,
      aws_sqs_queue.soh_order_item_tracking_id_received.arn,
      aws_sqs_queue.soh_order_item_tour_started.arn,
      aws_sqs_queue.soh_invoices_from_core.arn,
      aws_sqs_queue.soh_core_cancellation.arn,
      aws_sqs_queue.soh_subsequent_delivery_received.arn,
      aws_sqs_queue.d365_order_payment_secured.arn,
      aws_sqs_queue.soh_dropshipment_shipment_confirmed.arn,
      aws_sqs_queue.dropshipment_purchase_order_booked.arn
    ]
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
