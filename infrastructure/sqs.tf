locals {
  version = "v1"
  sqs_queues = {
    ecp_shop_orders = "${var.environment}-${local.service_prefix}-ecp-shop-orders-${local.version}",
    ecp_shop_orders_dlq = "${var.environment}-${local.service_prefix}-ecp-shop-orders-${local.version}-dlq",
    bc_shop_orders = "${var.environment}-${local.service_prefix}-bc-shop-orders-${local.version}",
    bc_shop_orders_dlq = "${var.environment}-${local.service_prefix}-bc-shop-orders-${local.version}-dlq",
    core_shop_orders = "${var.environment}-${local.service_prefix}-core-shop-orders-${local.version}",
    core_shop_orders_dlq = "${var.environment}-${local.service_prefix}-core-shop-orders-${local.version}-dlq",
    order_item_shipped = "${var.environment}-${local.service_prefix}-order-item-shipped-${local.version}",
    order_item_shipped_dlq = "${var.environment}-${local.service_prefix}-order-item-shipped-${local.version}-dlq",
    order_payment_secured = "${var.environment}-${local.service_prefix}-order-payment-secured-${local.version}"
    order_payment_secured_dlq = "${var.environment}-${local.service_prefix}-order-payment-secured-${local.version}-dlq"
    order_item_transmitted_to_logistic = "${var.environment}-${local.service_prefix}-order-item-transmitted-to-logistic-${local.version}"
    order_item_transmitted_to_logistic_dlq = "${var.environment}-${local.service_prefix}-order-item-transmitted-to-logistic-${local.version}-dlq"
    order_item_packing_started = "${var.environment}-${local.service_prefix}-order-item-packing-started-${local.version}"
    order_item_packing_started_dlq = "${var.environment}-${local.service_prefix}-order-item-packing-started-${local.version}-dlq"
    order_item_tracking_id_received = "${var.environment}-${local.service_prefix}-order-item-tracking-id-received-${local.version}"
    order_item_tracking_id_received_dlq = "${var.environment}-${local.service_prefix}-order-item-tracking-id-received-${local.version}-dlq"
    order_item_tour_started = "${var.environment}-${local.service_prefix}-order-item-tour-started-${local.version}"
    order_item_tour_started_dlq = "${var.environment}-${local.service_prefix}-order-item-tour-started-${local.version}-dlq",
    invoices_from_core = "${var.environment}-${local.service_prefix}-invoices-from-core-${local.version}",
    invoices_from_core_dlq = "${var.environment}-${local.service_prefix}-invoices-from-core-${local.version}-dlq",
    core_cancellation = "${var.environment}-${local.service_prefix}-core-cancellation-${local.version}",
    core_cancellation_dlq = "${var.environment}-${local.service_prefix}-core-cancellation-${local.version}-dlq",
    subsequent_delivery_received = "${var.environment}-${local.service_prefix}-subsequent-delivery-received-${local.version}",
    subsequent_delivery_received_dlq = "${var.environment}-${local.service_prefix}-subsequent-delivery-received-${local.version}-dlq",
    d365_order_payment_secured = "${var.environment}-${local.service_prefix}-d365_order_payment_secured-${local.version}",
    d365_order_payment_secured_dlq = "${var.environment}-${local.service_prefix}-d365_order_payment_secured-${local.version}-dlq",
    dropshipment_shipment_confirmed = "${var.environment}-${local.service_prefix}-dropshipment-shipment-confirmed-${local.version}",
    dropshipment_shipment_confirmed_dlq = "${var.environment}-${local.service_prefix}-dropshipment-shipment-confirmed-${local.version}-dlq",
    dropshipment_purchase_order_booked = "${var.environment}-${local.service_prefix}-dropshipment-purchase-order-booked-${local.version}",
    dropshipment_purchase_order_booked_dlq = "${var.environment}-${local.service_prefix}-dropshipment-purchase-order-booked-${local.version}-dlq",
    dropshipment_purchase_order_return_notified="${var.environment}-${local.service_prefix}-dropshipment-purchase-order-return-notified-${local.version}",
    dropshipment_purchase_order_return_notified_dlq="${var.environment}-${local.service_prefix}-dropshipment-purchase-order-return-notified-${local.version}-dlq",
    dropshipment_purchase_order_return_confirmed = "${var.environment}-${local.service_prefix}-dropshipment-purchase-order-return-confirmed-${local.version}",
    dropshipment_purchase_order_return_confirmed_dlq = "${var.environment}-${local.service_prefix}-dropshipment-purchase-order-return-confirmed-${local.version}-dlq",
    core_sales_credit_note_created = "${var.environment}-${local.service_prefix}-core-sales-credit-note-created-${local.version}",
    core_sales_credit_note_created_dlq = "${var.environment}-${local.service_prefix}-core-sales-credit-note-created-${local.version}-dlq",
    core_sales_invoice_created = "${var.environment}-${local.service_prefix}-core-sales-invoice-created-${local.version}",
    core_sales_invoice_created_dlq = "${var.environment}-${local.service_prefix}-core-sales-invoice-created-${local.version}-dlq",
    migration_core_sales_order_created = "${var.environment}-${local.service_prefix}-migration-core-sales-order-created-${local.version}",
    migration_core_sales_order_created_dlq = "${var.environment}-${local.service_prefix}-migration-core-sales-order-created-${local.version}-dlq",
    migration_core_sales_invoice_created = "${var.environment}-${local.service_prefix}-migration-core-sales-invoice-created-${local.version}",
    migration_core_sales_invoice_created_dlq = "${var.environment}-${local.service_prefix}-migration-core_sales-invoice-created-${local.version}-dlq",
    migration_core_sales_credit_note_created = "${var.environment}-${local.service_prefix}-migration-core-sales-credit-note-created-${local.version}",
    migration_core_sales_credit_note_created_dlq = "${var.environment}-${local.service_prefix}-migration-core-sales-credit-note-created-${local.version}-dlq",
    parcel_shipped = "${var.environment}-${local.service_prefix}-parcel-shipped-${local.version}",
    parcel_shipped_dlq = "${var.environment}-${local.service_prefix}-parcel-shipped-${local.version}-dlq",
    tmp_core_sales_order_created = "${var.environment}-${local.service_prefix}-tmp-core-sales-order-created-${local.version}",
    tmp_core_sales_order_created_dlq = "${var.environment}-${local.service_prefix}-tmp-core-sales-order-created-${local.version}-dlq",
    tmp_core_sales_invoice_created = "${var.environment}-${local.service_prefix}-tmp-core-sales-invoice-created-${local.version}",
    tmp_core_sales_invoice_created_dlq = "${var.environment}-${local.service_prefix}-tmp-core-sales-invoice-created-${local.version}-dlq",
    tmp_core_sales_credit_note_created = "${var.environment}-${local.service_prefix}-tmp-core-sales-credit-note-created-${local.version}",
    tmp_core_sales_credit_note_created_dlq = "${var.environment}-${local.service_prefix}-tmp-core-sales-credit-note-created-${local.version}-dlq"
  }
}

resource "aws_sqs_queue" "ecp_shop_orders_dlq" {
  name = local.sqs_queues.ecp_shop_orders_dlq
}

resource "aws_sqs_queue" "ecp_shop_orders" {
  name = local.sqs_queues.ecp_shop_orders
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.ecp_shop_orders_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "bc_shop_orders_dlq" {
  name = local.sqs_queues.bc_shop_orders_dlq
}

resource "aws_sqs_queue" "bc_shop_orders" {
  name = local.sqs_queues.bc_shop_orders
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.bc_shop_orders_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "core_shop_orders_dlq" {
  name = local.sqs_queues.core_shop_orders_dlq
}

resource "aws_sqs_queue" "core_shop_orders" {
  name = local.sqs_queues.core_shop_orders
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.core_shop_orders_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_order_item_shipped_dlq" {
  name = local.sqs_queues.order_item_shipped_dlq
}

resource "aws_sqs_queue" "soh_order_item_shipped" {
  name = local.sqs_queues.order_item_shipped
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_shipped_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_order_payment_secured_dlq" {
  name = local.sqs_queues.order_payment_secured_dlq
}

resource "aws_sqs_queue" "soh_order_payment_secured" {
  name = local.sqs_queues.order_payment_secured
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_payment_secured_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_order_item_transmitted_to_logistic_dlq" {
  name = local.sqs_queues.order_item_transmitted_to_logistic_dlq
}

resource "aws_sqs_queue" "soh_order_item_transmitted_to_logistic" {
  name = local.sqs_queues.order_item_transmitted_to_logistic
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_transmitted_to_logistic_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_order_item_packing_started_dlq" {
  name = local.sqs_queues.order_item_packing_started_dlq
}

resource "aws_sqs_queue" "soh_order_item_packing_started" {
  name = local.sqs_queues.order_item_packing_started
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_packing_started_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_order_item_tracking_id_received_dlq" {
  name = local.sqs_queues.order_item_tracking_id_received_dlq
}

resource "aws_sqs_queue" "soh_order_item_tracking_id_received" {
  name = local.sqs_queues.order_item_tracking_id_received
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_tracking_id_received_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_order_item_tour_started_dlq" {
  name = local.sqs_queues.order_item_tour_started_dlq
}

resource "aws_sqs_queue" "soh_order_item_tour_started" {
  name = local.sqs_queues.order_item_tour_started
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_order_item_tour_started_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_invoices_from_core_dlq" {
  name = local.sqs_queues.invoices_from_core_dlq
}

resource "aws_sqs_queue" "soh_invoices_from_core" {
  name = local.sqs_queues.invoices_from_core
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_invoices_from_core_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_core_cancellation_dlq" {
  name = local.sqs_queues.core_cancellation_dlq
}

resource "aws_sqs_queue" "soh_core_cancellation" {
  name = local.sqs_queues.core_cancellation
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_core_cancellation_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_subsequent_delivery_received_dlq" {
  name = local.sqs_queues.subsequent_delivery_received_dlq
}

resource "aws_sqs_queue" "soh_subsequent_delivery_received" {
  name = local.sqs_queues.subsequent_delivery_received
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_subsequent_delivery_received_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "d365_order_payment_secured_dlq" {
  name = local.sqs_queues.d365_order_payment_secured_dlq
}

resource "aws_sqs_queue" "d365_order_payment_secured" {
  name = local.sqs_queues.d365_order_payment_secured
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.d365_order_payment_secured_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_dropshipment_shipment_confirmed_dlq" {
  name = local.sqs_queues.dropshipment_shipment_confirmed_dlq
}

resource "aws_sqs_queue" "soh_dropshipment_shipment_confirmed" {
  name = local.sqs_queues.dropshipment_shipment_confirmed
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_dropshipment_shipment_confirmed_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "dropshipment_purchase_order_booked_dlq" {
  name = local.sqs_queues.dropshipment_purchase_order_booked_dlq
}

resource "aws_sqs_queue" "dropshipment_purchase_order_booked" {
  name = local.sqs_queues.dropshipment_purchase_order_booked
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.dropshipment_purchase_order_booked_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_dropshipment_purchase_order_return_notified_dlq" {
  name = local.sqs_queues.dropshipment_purchase_order_return_notified_dlq
}

resource "aws_sqs_queue" "soh_dropshipment_purchase_order_return_notified" {
  name = local.sqs_queues.dropshipment_purchase_order_return_notified
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_dropshipment_purchase_order_return_notified_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "dropshipment_purchase_order_return_confirmed_dlq" {
  name = local.sqs_queues.dropshipment_purchase_order_return_confirmed_dlq
}

resource "aws_sqs_queue" "dropshipment_purchase_order_return_confirmed" {
  name = local.sqs_queues.dropshipment_purchase_order_return_confirmed
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.dropshipment_purchase_order_return_confirmed_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_core_sales_credit_note_created_dlq" {
  name = local.sqs_queues.core_sales_credit_note_created_dlq
}

resource "aws_sqs_queue" "soh_core_sales_credit_note_created" {
  name = local.sqs_queues.core_sales_credit_note_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_core_sales_credit_note_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_core_sales_invoice_created_dlq" {
  name = local.sqs_queues.core_sales_invoice_created_dlq
}

resource "aws_sqs_queue" "soh_core_sales_invoice_created" {
  name = local.sqs_queues.core_sales_invoice_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_core_sales_invoice_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_migration_core_sales_order_created_dlq" {
  name = local.sqs_queues.migration_core_sales_order_created_dlq
}

resource "aws_sqs_queue" "soh_migration_core_sales_order_created" {
  name = local.sqs_queues.migration_core_sales_order_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_migration_core_sales_order_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_migration_core_sales_invoice_created_dlq" {
  name = local.sqs_queues.migration_core_sales_invoice_created_dlq
}

resource "aws_sqs_queue" "soh_migration_core_sales_invoice_created" {
  name = local.sqs_queues.migration_core_sales_invoice_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_migration_core_sales_invoice_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_migration_core_sales_credit_note_created_dlq" {
  name = local.sqs_queues.migration_core_sales_credit_note_created_dlq
}

resource "aws_sqs_queue" "soh_migration_core_sales_credit_note_created" {
  name = local.sqs_queues.migration_core_sales_credit_note_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_migration_core_sales_credit_note_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_parcel_shipped_dlq" {
  name = local.sqs_queues.parcel_shipped_dlq
}

resource "aws_sqs_queue" "soh_parcel_shipped" {
  name = local.sqs_queues.parcel_shipped
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_parcel_shipped_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}


resource "aws_sqs_queue" "soh_tmp_core_sales_order_created_dlq" {
  name = local.sqs_queues.tmp_core_sales_order_created_dlq
}

resource "aws_sqs_queue" "soh_tmp_core_sales_order_created" {
  name = local.sqs_queues.tmp_core_sales_order_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_tmp_core_sales_order_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_tmp_core_sales_invoice_created_dlq" {
  name = local.sqs_queues.tmp_core_sales_invoice_created_dlq
}

resource "aws_sqs_queue" "soh_tmp_core_sales_invoice_created" {
  name = local.sqs_queues.tmp_core_sales_invoice_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_tmp_core_sales_invoice_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}

resource "aws_sqs_queue" "soh_tmp_core_sales_credit_note_created_dlq" {
  name = local.sqs_queues.tmp_core_sales_credit_note_created_dlq
}

resource "aws_sqs_queue" "soh_tmp_core_sales_credit_note_created" {
  name = local.sqs_queues.tmp_core_sales_credit_note_created
  redrive_policy  = "{\"deadLetterTargetArn\":\"${aws_sqs_queue.soh_tmp_core_sales_credit_note_created_dlq.arn}\",\"maxReceiveCount\":4}"
  visibility_timeout_seconds = 120
}