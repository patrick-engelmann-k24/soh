locals {
  version = "v1"
  sqs_queues = {
    ecp_shop_orders = "${var.environment}-${local.service_prefix}-ecp-shop-orders-${local.version}",
    order_item_shipped = "soh-order-item-shipped-${local.version}"
  }
}

resource "aws_sqs_queue" "ecp_shop_orders" {
  name = local.sqs_queues.ecp_shop_orders
}

resource "aws_sqs_queue" "soh_order_item_shipped" {
  name = local.sqs_queues.order_item_shipped
}