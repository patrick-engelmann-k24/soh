locals {
  version = "v1"
  sqs_queues = {
    ecp_shop_orders = "${var.environment}-${local.service_prefix}-ecp-shop-orders-${local.version}",
    order_item_shipped = "${var.environment}-${local.service_prefix}-order-item-shipped-${local.version}",
    order_payment_secured = "${var.environment}-${local.service_prefix}-order-payment-secured-${local.version}"
    order_item_transmitted_to_logistic = "${var.environment}-${local.service_prefix}-order-item-transmitted-to-logistic-${local.version}"
    order_item_packing_started = "${var.environment}-${local.service_prefix}-order-item-packing-started-${local.version}"
    order_item_tracking_id_received = "${var.environment}-${local.service_prefix}-order-item-tracking-id-received-${local.version}"
    order_item_tour_started = "${var.environment}-${local.service_prefix}-order-item-tour-started-${local.version}"
  }
}

resource "aws_sqs_queue" "ecp_shop_orders_dlq" {
  name = local.sqs_queues.ecp_shop_orders
}

resource "aws_sqs_queue" "ecp_shop_orders" {
  name = local.sqs_queues.ecp_shop_orders
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.ecp_shop_orders_dlq.arn}\",\"maxReceiveCount\":4}"
}

resource "aws_sqs_queue" "soh_order_item_shipped_dlq" {
  name = local.sqs_queues.order_item_shipped
}

resource "aws_sqs_queue" "soh_order_item_shipped" {
  name = local.sqs_queues.order_item_shipped
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_shipped_dlq.arn}\",\"maxReceiveCount\":4}"
}

resource "aws_sqs_queue" "soh_order_payment_secured_dlq" {
  name = local.sqs_queues.order_payment_secured
}

resource "aws_sqs_queue" "soh_order_payment_secured" {
  name = local.sqs_queues.order_payment_secured
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_payment_secured_dlq.arn}\",\"maxReceiveCount\":4}"
}

resource "aws_sqs_queue" "soh_order_item_transmitted_to_logistic_dlq" {
  name = local.sqs_queues.order_item_transmitted_to_logistic
}

resource "aws_sqs_queue" "soh_order_item_transmitted_to_logistic" {
  name = local.sqs_queues.order_item_transmitted_to_logistic
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_transmitted_to_logistic_dlq.arn}\",\"maxReceiveCount\":4}"
}

resource "aws_sqs_queue" "soh_order_item_packing_started_dlq" {
  name = local.sqs_queues.order_item_packing_started
}

resource "aws_sqs_queue" "soh_order_item_packing_started" {
  name = local.sqs_queues.order_item_packing_started
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_packing_started_dlq.arn}\",\"maxReceiveCount\":4}"
}

resource "aws_sqs_queue" "soh_order_item_tracking_id_received_dlq" {
  name = local.sqs_queues.order_item_tracking_id_received
}

resource "aws_sqs_queue" "soh_order_item_tracking_id_received" {
  name = local.sqs_queues.order_item_tracking_id_received
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_tracking_id_received_dlq.arn}\",\"maxReceiveCount\":4}"
}

resource "aws_sqs_queue" "soh_order_item_tour_started_dlq" {
  name = local.sqs_queues.order_item_tour_started
}

resource "aws_sqs_queue" "soh_order_item_tour_started" {
  name = local.sqs_queues.order_item_tour_started
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_tour_started_dlq.arn}\",\"maxReceiveCount\":4}"
}