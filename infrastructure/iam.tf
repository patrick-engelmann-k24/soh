data "aws_iam_policy_document" "sns_sqs_access_policy" {
  statement {
    effect = "Allow"

    actions = [
      "sns:*",
    ]

    resources = [
      data.aws_sns_topic.sns_soh_order_created_topic.arn,
    ]
  }
  statement {
    effect = "Allow"

    actions = [
      "sqs:*",
    ]

    resources = [
      aws_sqs_queue.ecp_shop_orders.arn,
      aws_sqs_queue.soh_order_item_shipped.arn,
      aws_sqs_queue.soh_order_payment_secured.arn,
      aws_sqs_queue.soh_order_item_transmitted_to_logistic.arn,
      aws_sqs_queue.soh_order_item_packing_started.arn,
      aws_sqs_queue.soh_order_item_tracking_id_received.arn,
      aws_sqs_queue.soh_order_item_tour_started.arn
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
