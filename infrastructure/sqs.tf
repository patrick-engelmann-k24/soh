locals {
  version = "v1"
  sqs_queues = {
    ecp_shop_orders = "${var.environment}-${local.service_prefix}-ecp-shop-orders-${local.version}"
  }
}

resource "aws_sqs_queue" "ecp_shop_orders" {
  name = local.sqs_queues.ecp_shop_orders
}