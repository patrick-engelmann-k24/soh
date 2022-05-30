data "aws_sns_topic" "sns_soh_order_created_v2_topic" {
  name = "soh-order-created-v2"
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

data "aws_sns_topic" "sns_soh_sales_order_completed_topic_v1" {
  name = "soh-sales-order-completed-v1"
}

data "aws_sns_topic" "sns_soh_order_item_cancelled_topic" {
  name = "soh-order-item-cancelled"
}

data "aws_sns_topic" "sns_soh_order_cancelled_topic" {
  name = "soh-order-cancelled"
}

data "aws_sns_topic" "sns_soh_order_rows_cancelled_topic_v1" {
  name = "soh-order-rows-cancelled-v1"
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

data "aws_sns_topic" "sns_soh_order_item_tour_started" {
  name = "soh-order-item-tour-started"
}

data "aws_sns_topic" "sns_braincraft_order_received" {
  name = "braincraft-order-received-v1"
}

data "aws_sns_topic" "sns_core_sales_orders_created" {
  name = "core-sales-order-created-v1"
}

data "aws_sns_topic" "sns_core_cancellation_delivery_note_printed_v1" {
  name = "core-cancellation-delivery-note-printed-v1"
}

data "aws_sns_topic" "sns_soh_sales_order_row_cancellation_v1" {
  name = "soh-sales-order-row-cancelled-v1"
}

data "aws_sns_topic" "sns_soh_sales_order_cancellation_v1" {
  name = "soh-sales-order-cancelled-v1"
}

data "aws_sns_topic" "sns_core_subsequent_delivery_note_printed" {
  name = "core-subsequent-delivery-note-printed-v1"
}

data "aws_sns_topic" "sns_core_sales_invoice_created" {
  name = "core-sales-invoice-created-v1"
}

data "aws_sns_topic" "sns_soh_order_invoice_created_v1" {
  name = "soh-order-invoice-created-v1"
}

data "aws_sns_topic" "sns_dropshipment_shipment_confirmed_v1" {
  name = "dropshipment-shipment-confirmed-v1"
}

data "aws_sns_topic" "sns_soh_shipment_confirmed_v1" {
  name = "soh-shipment-confirmed-v1"
}

data "aws_sns_topic" "sns_soh_dropshipment_purchase_order_booked_v1" {
  name = "dropshipment-purchase-order-booked-v1"
}

data "aws_sns_topic" "sns_core_sales_credit_note_created_v1" {
  name = "core-sales-credit-note-created-v1"
}

data "aws_sns_topic" "sns_soh_dropshipment_order_created_v1" {
  name = "soh-dropshipment-order-created-v1"
}

data "aws_sns_topic" "sns_soh_return_order_created_v1" {
  name = "soh-return-order-created-v1"
}

data "aws_sns_topic" "sns_soh_core_invoice_received_v1" {
  name = "soh-core-invoice-received-v1"
}

data "aws_sns_topic" "sns_soh_credit_note_received_v1" {
  name = "soh-credit-note-received-v1"
}

data "aws_sns_topic" "sns_migration_core_sales_order_created_v1" {
  name = "migration-core-sales-order-created-v1"
}

data "aws_sns_topic" "sns_migration_soh_order_created_v2" {
  name = "migration-soh-order-created-v2"
}

data "aws_sns_topic" "sns_migration_soh_sales_order_row_cancelled_v1" {
  name = "migration-soh-sales-order-row-cancelled-v1"
}

data "aws_sns_topic" "sns_migration_soh_sales_order_cancelled_v1" {
  name = "migration-soh-sales-order-cancelled-v1"
}

data "aws_sns_topic" "sns_migration_core_sales_invoice_created" {
  name = "migration-core-sales-invoice-created-v1"
}

data "aws_sns_topic" "sns_migration_soh_return_order_created_v1" {
  name = "migration-soh-return-order-created-v1"
}

data "aws_sns_topic" "sns_migration_core_sales_credit_note_created" {
  name = "migration-core-sales-credit-note-created-v1"
}

data "aws_sns_topic" "sns_dropshipment_purchase_order_return_notified" {
  name = "dropshipment-purchase-order-return-notified-v1"
}

data "aws_sns_topic" "sns_soh_dropshipment_order_return_notified_v1" {
  name = "soh-dropshipment-order-return-notified-v1"
}

# subscriptions of sqs to sns
resource "aws_sns_topic_subscription" "sns_subscription_ecp_orders_v3" {
  endpoint = aws_sqs_queue.ecp_shop_orders.arn
  protocol = "sqs"
  topic_arn = var.ecp_new_order_sns_v3
}

# subscription for de-shop orders
resource "aws_sns_topic_subscription" "sns_subscription_braincraft_orders" {
  endpoint = aws_sqs_queue.bc_shop_orders.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_braincraft_order_received.arn
}

# subscription for core aka offline orders
resource "aws_sns_topic_subscription" "sns_subscription_core_orders" {
  endpoint = aws_sqs_queue.core_shop_orders.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_core_sales_orders_created.arn
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

resource "aws_sns_topic_subscription" "sns_subscription_order_item_packing_started" {
  endpoint = aws_sqs_queue.soh_order_item_packing_started.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_soh_packing_started_topic.arn
}

resource "aws_sns_topic_subscription" "sns_subscription_order_item_tracking_id_received" {
  endpoint = aws_sqs_queue.soh_order_item_tracking_id_received.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_soh_tracking_id_received_topic.arn
}

resource "aws_sns_topic_subscription" "sns_subscription_order_item_tour_started" {
  endpoint = aws_sqs_queue.soh_order_item_tour_started.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_soh_order_item_tour_started.arn
}

# subscribe for invoices
resource "aws_sns_topic_subscription" "sns_subscription_invoices_from_core" {
  endpoint = aws_sqs_queue.soh_invoices_from_core.arn
  protocol = "sqs"
  topic_arn = var.invoices_from_core_sns
}

resource "aws_sns_topic_subscription" "sns_subscription_core_cancellation" {
  endpoint = aws_sqs_queue.soh_core_cancellation.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_core_cancellation_delivery_note_printed_v1.arn
}

# subscription for core subsequent delivery notes
resource "aws_sns_topic_subscription" "sns_subscription_subsequent_delivery_note" {
  endpoint = aws_sqs_queue.soh_subsequent_delivery_received.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_core_subsequent_delivery_note_printed.arn
}

# subscription for core sales invoice created
resource "aws_sns_topic_subscription" "sns_subscription_core_invoice_created" {
  endpoint = aws_sqs_queue.soh_core_sales_invoice_created.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_core_sales_invoice_created.arn
}

# subscription for payment secured published by ECP
resource "aws_sns_topic_subscription" "sns_subscription_d365_order_payment_secured" {
  endpoint = aws_sqs_queue.d365_order_payment_secured.arn
  protocol = "sqs"
  topic_arn = var.d365_order_payment_secured_sns
}

# subscription for dropshipment shipment confirmed published by P&R
resource "aws_sns_topic_subscription" "sns_subscription_dropshipment_shipment_confirmed" {
  endpoint = aws_sqs_queue.soh_dropshipment_shipment_confirmed.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_dropshipment_shipment_confirmed_v1.arn
}

resource "aws_sns_topic_subscription" "dropshipment-purchase_order_booked" {
  endpoint  = aws_sqs_queue.dropshipment_purchase_order_booked.arn
  protocol  = "sqs"
  topic_arn = data.aws_sns_topic.sns_soh_dropshipment_purchase_order_booked_v1.arn
}

# subscription for core sales credit note created published by core-publisher
resource "aws_sns_topic_subscription" "sns_subscription_core-sales-credit-note-created" {
  endpoint = aws_sqs_queue.soh_core_sales_credit_note_created.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_core_sales_credit_note_created_v1.arn
}

resource "aws_sns_topic_subscription" "sns_subscription_core_sales_invoice_created_v1" {
  endpoint  = aws_sqs_queue.soh_migration_core_sales_order_created.arn
  protocol  = "sqs"
  topic_arn = data.aws_sns_topic.sns_migration_core_sales_order_created_v1.arn
}

resource "aws_sns_topic_subscription" "sns_subscription_migration_sales_invoice_created" {
  endpoint = aws_sqs_queue.soh_migration_core_sales_invoice_created.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_migration_core_sales_invoice_created.arn
}

# subscription for migration core sales credit note created
resource "aws_sns_topic_subscription" "sns_subscription_migration_core_sales_credit_note_created" {
  endpoint = aws_sqs_queue.soh_migration_core_sales_credit_note_created.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_migration_core_sales_credit_note_created.arn
}

# subscription for dropshipment purchase order return notified
resource "aws_sns_topic_subscription" "sns_subscription_dropshipment_purchase_order_return_notified" {
  endpoint = aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.arn
  protocol = "sqs"
  topic_arn = data.aws_sns_topic.sns_dropshipment_purchase_order_return_notified.arn
}

