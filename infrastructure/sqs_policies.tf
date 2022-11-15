locals {
  sqs_queues_info = {
    ecp_shop_orders = tomap({
      id  = aws_sqs_queue.ecp_shop_orders.id
      arn = aws_sqs_queue.ecp_shop_orders.arn
    })
    ecp_shop_orders_dlq = tomap({
      id  = aws_sqs_queue.ecp_shop_orders_dlq.id
      arn = aws_sqs_queue.ecp_shop_orders_dlq.arn
    })
    bc_shop_orders = tomap({
      id  = aws_sqs_queue.bc_shop_orders.id
      arn = aws_sqs_queue.bc_shop_orders.arn
    })
    bc_shop_orders_dlq = tomap({
      id  = aws_sqs_queue.bc_shop_orders_dlq.id
      arn = aws_sqs_queue.bc_shop_orders_dlq.arn
    })
    core_shop_orders = tomap({
      id  = aws_sqs_queue.core_shop_orders.id
      arn = aws_sqs_queue.core_shop_orders.arn
    })
    core_shop_orders_dlq = tomap({
      id  = aws_sqs_queue.core_shop_orders_dlq.id
      arn = aws_sqs_queue.core_shop_orders_dlq.arn
    })
    soh_order_payment_secured = tomap({
      id  = aws_sqs_queue.soh_order_payment_secured.id
      arn = aws_sqs_queue.soh_order_payment_secured.arn
    })
    soh_order_payment_secured_dlq = tomap({
      id  = aws_sqs_queue.soh_order_payment_secured_dlq.id
      arn = aws_sqs_queue.soh_order_payment_secured_dlq.arn
    })
    soh_invoices_from_core = tomap({
      id  = aws_sqs_queue.soh_invoices_from_core.id
      arn = aws_sqs_queue.soh_invoices_from_core.arn
    })
    invoices_from_core_dlq = tomap({
      id  = aws_sqs_queue.soh_invoices_from_core_dlq.id
      arn = aws_sqs_queue.soh_invoices_from_core_dlq.arn
    })
    d365_order_payment_secured = tomap({
      id  = aws_sqs_queue.d365_order_payment_secured.id
      arn = aws_sqs_queue.d365_order_payment_secured.arn
    })
    d365_order_payment_secured_dlq = tomap({
      id  = aws_sqs_queue.d365_order_payment_secured_dlq.id
      arn = aws_sqs_queue.d365_order_payment_secured_dlq.arn
    })
    dropshipment_shipment_confirmed = tomap({
      id  = aws_sqs_queue.soh_dropshipment_shipment_confirmed.id
      arn = aws_sqs_queue.soh_dropshipment_shipment_confirmed.arn
    })
    dropshipment_shipment_confirmed_dlq = tomap({
      id  = aws_sqs_queue.soh_dropshipment_shipment_confirmed_dlq.id
      arn = aws_sqs_queue.soh_dropshipment_shipment_confirmed_dlq.arn
    })
    dropshipment_purchase_order_booked = tomap({
      id  = aws_sqs_queue.soh_dropshipment_purchase_order_booked.id
      arn = aws_sqs_queue.soh_dropshipment_purchase_order_booked.arn
    })
    dropshipment_purchase_order_booked_dlq = tomap({
      id  = aws_sqs_queue.soh_dropshipment_purchase_order_booked_dlq.id
      arn = aws_sqs_queue.soh_dropshipment_purchase_order_booked_dlq.arn
    })
    dropshipment_purchase_order_return_notified = tomap({
      id  = aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.id
      arn = aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.arn
    })
    dropshipment_purchase_order_return_notified_dlq = tomap({
      id  = aws_sqs_queue.soh_dropshipment_purchase_order_return_notified_dlq.id
      arn = aws_sqs_queue.soh_dropshipment_purchase_order_return_notified_dlq.arn
    })
    dropshipment_purchase_order_return_confirmed = tomap({
      id  = aws_sqs_queue.soh_dropshipment_purchase_order_return_confirmed.id
      arn = aws_sqs_queue.soh_dropshipment_purchase_order_return_confirmed.arn
    })
    dropshipment_purchase_order_return_confirmed_dlq = tomap({
      id  = aws_sqs_queue.soh_dropshipment_purchase_order_return_confirmed_dlq.id
      arn = aws_sqs_queue.soh_dropshipment_purchase_order_return_confirmed_dlq.arn
    })
    core_sales_credit_note_created = tomap({
      id  = aws_sqs_queue.soh_core_sales_credit_note_created.id
      arn = aws_sqs_queue.soh_core_sales_credit_note_created.arn
    })
    core_sales_credit_note_created_dlq = tomap({
      id  = aws_sqs_queue.soh_core_sales_credit_note_created_dlq.id
      arn = aws_sqs_queue.soh_core_sales_credit_note_created_dlq.arn
    })
    core_sales_invoice_created = tomap({
      id  = aws_sqs_queue.soh_core_sales_invoice_created.id
      arn = aws_sqs_queue.soh_core_sales_invoice_created.arn
    })
    core_sales_invoice_created_dlq = tomap({
      id  = aws_sqs_queue.soh_core_sales_invoice_created_dlq.id
      arn = aws_sqs_queue.soh_core_sales_invoice_created_dlq.arn
    })
    migration_core_sales_order_created = tomap({
      id  = aws_sqs_queue.soh_migration_core_sales_order_created.id
      arn = aws_sqs_queue.soh_migration_core_sales_order_created.arn
    })
    migration_core_sales_order_created_dlq = tomap({
      id  = aws_sqs_queue.soh_migration_core_sales_order_created_dlq.id
      arn = aws_sqs_queue.soh_migration_core_sales_order_created_dlq.arn
    })
    migration_core_sales_invoice_created = tomap({
      id  = aws_sqs_queue.soh_migration_core_sales_invoice_created.id
      arn = aws_sqs_queue.soh_migration_core_sales_invoice_created.arn
    })
    migration_core_sales_invoice_created_dlq = tomap({
      id  = aws_sqs_queue.soh_migration_core_sales_invoice_created_dlq.id
      arn = aws_sqs_queue.soh_migration_core_sales_invoice_created_dlq.arn
    })
    migration_core_sales_credit_note_created = tomap({
      id  = aws_sqs_queue.soh_migration_core_sales_credit_note_created.id
      arn = aws_sqs_queue.soh_migration_core_sales_credit_note_created.arn
    })
    migration_core_sales_credit_note_created_dlq = tomap({
      id  = aws_sqs_queue.soh_migration_core_sales_credit_note_created_dlq.id
      arn = aws_sqs_queue.soh_migration_core_sales_credit_note_created_dlq.arn
    })
    parcel_shipped = tomap({
      id  = aws_sqs_queue.soh_parcel_shipped.id
      arn = aws_sqs_queue.soh_parcel_shipped.arn
    })
    parcel_shipped_dlq = tomap({
      id  = aws_sqs_queue.soh_parcel_shipped_dlq.id
      arn = aws_sqs_queue.soh_parcel_shipped_dlq.arn
    })
    tmp_core_sales_order_created = tomap({
      id  = aws_sqs_queue.soh_tmp_core_sales_order_created.id
      arn = aws_sqs_queue.soh_tmp_core_sales_order_created.arn
    })
    tmp_core_sales_order_created_dlq = tomap({
      id  = aws_sqs_queue.soh_tmp_core_sales_order_created_dlq.id
      arn = aws_sqs_queue.soh_tmp_core_sales_order_created_dlq.arn
    })
    tmp_core_sales_invoice_created = tomap({
      id  = aws_sqs_queue.soh_tmp_core_sales_credit_note_created.id
      arn = aws_sqs_queue.soh_tmp_core_sales_credit_note_created.arn
    })
    tmp_core_sales_invoice_created_dlq = tomap({
      id  = aws_sqs_queue.soh_tmp_core_sales_invoice_created_dlq.id
      arn = aws_sqs_queue.soh_tmp_core_sales_invoice_created_dlq.arn
    })
    tmp_core_sales_credit_note_created = tomap({
      id  = aws_sqs_queue.soh_tmp_core_sales_credit_note_created.id
      arn = aws_sqs_queue.soh_tmp_core_sales_credit_note_created.arn
    })
    tmp_core_sales_credit_note_created_dlq = tomap({
      id  = aws_sqs_queue.soh_tmp_core_sales_credit_note_created_dlq.id
      arn = aws_sqs_queue.soh_tmp_core_sales_credit_note_created_dlq.arn
    })
    paypal_refund_instruction_successful = tomap({
      id  = aws_sqs_queue.soh_paypal_refund_instruction_successful.id
      arn = aws_sqs_queue.soh_paypal_refund_instruction_successful.arn
    })
    paypal_refund_instruction_successful_dlq = tomap({
      id  = aws_sqs_queue.soh_paypal_refund_instruction_successful_dlq.id
      arn = aws_sqs_queue.soh_paypal_refund_instruction_successful_dlq.arn
    })
    core_sales_order_cancelled = tomap({
      id  = aws_sqs_queue.soh_core_sales_order_cancelled.id
      arn = aws_sqs_queue.soh_core_sales_order_cancelled.arn
    })
    paypal_refund_instruction_successful_dlq = tomap({
      id  = aws_sqs_queue.soh_core_sales_order_cancelled_dlq.id
      arn = aws_sqs_queue.soh_core_sales_order_cancelled_dlq.arn
    })
  }
}

data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "sns_sqs_send_message_policy_document" {

  for_each = local.sqs_queues_info

  statement {
    sid    = "SNS-${each.key}"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      (each.value).arn
    ]
  }

  statement {
    effect = "Allow"

    actions = [
      "sqs:*",
    ]

    principals {
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
      type        = "AWS"
    }

    resources = [
      (each.value).arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_send_message_policy" {
  for_each  = local.sqs_queues_info
  queue_url = (each.value).id
  policy    = data.aws_iam_policy_document.sns_sqs_send_message_policy_document[each.key].json
}
