data "aws_sns_topic" "sns_soh_order_created_topic" {
  name = "soh-order-created"
}

data "aws_sns_topic" "sns_soh_sales_order_canceled_topic" {
  name = "soh-sales-order-canceled"
}

data "aws_sns_topic" "sns_soh_tracking_id_received_topic" {
  name = "soh-tracking-id-received"
}

data "aws_sns_topic" "sns_soh_item_transmitted_topic" {
  name = "soh-item-transmitted"
}

data "aws_sns_topic" "sns_soh_packing_started_topic" {
  name = "soh-packing-started"
}

data "aws_sns_topic" "sns_soh_order_completed_topic" {
  name = "soh-order-completed"
}

data "aws_sns_topic" "sns_soh_order_item_cancelled_topic" {
  name = "soh-order-item-cancelled"
}

data "aws_sns_topic" "sns_soh_order_cancelled_topic" {
  name = "soh-order-cancelled"
}

data "aws_sns_topic" "sns_soh_invoice_address_changed_topic" {
  name = "soh-invoice-address-changed"
}

data "aws_sns_topic" "sns_soh_delivery_address_changed_topic" {
  name = "soh-delivery-address-changed"
}

data "aws_sns_topic" "sns_soh_order_item_shipped" {
  name = "soh-order-item-shipped"
}

data "aws_sns_topic" "sns_soh_order_payment_secured" {
  name = "soh-order-payment-secured"
}

# subscriptions of sqs to sns
resource "aws_sns_topic_subscription" "sns_subscription_ecp_orders" {
  endpoint = aws_sqs_queue.ecp_shop_orders.arn
  protocol = "sqs"
  topic_arn = var.ecp_new_order_sns
}

resource "aws_sns_topic_subscription" "sns_subscription_order_item_shipped" {
  endpoint = aws_sqs_queue.soh_order_item_shipped.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_soh_order_item_shipped.arn
}

resource "aws_sns_topic_subscription" "sns_subscription_order_payment_secured" {
  endpoint = aws_sqs_queue.soh_order_payment_secured.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_soh_order_payment_secured.arn
}

resource "aws_sns_topic_subscription" "sns_subscription_order_item_transmitted_to_logistic" {
  endpoint = aws_sqs_queue.soh_order_item_transmitted_to_logistic.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_soh_item_transmitted_topic.arn
}