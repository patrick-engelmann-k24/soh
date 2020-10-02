data "aws_sns_topic" "sns_soh_order_created_topic" {
  name = "soh-order-created"
}

resource "aws_sns_topic_subscription" "resource_name" {
  endpoint = aws_sqs_queue.ecp_shop_orders.arn
  protocol = "sqs"
  topic_arn = var.ecp_new_order_sns
}