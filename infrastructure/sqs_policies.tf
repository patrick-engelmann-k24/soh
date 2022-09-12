data "aws_caller_identity" "current" {}

#policies
#ecp_shop_orders
data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_ecp_shop_orders" {
  statement {
    sid = "SNS-ecp-shop-orders"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.ecp_shop_orders.arn
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
      aws_sqs_queue.ecp_shop_orders.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_ecp_order" {
  queue_url = aws_sqs_queue.ecp_shop_orders.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_ecp_shop_orders.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_bc_shop_orders" {
  statement {
    sid = "SNS-bc-shop-orders"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.bc_shop_orders.arn
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
      aws_sqs_queue.bc_shop_orders.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_bc_order" {
  queue_url = aws_sqs_queue.bc_shop_orders.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_bc_shop_orders.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_core_shop_orders" {
  statement {
    sid = "SNS-core-shop-orders"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.core_shop_orders.arn
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
      aws_sqs_queue.core_shop_orders.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_core_order" {
  queue_url = aws_sqs_queue.core_shop_orders.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_core_shop_orders.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_order_item_shipped" {
  statement {
    sid = "SNS-order-item-shipped"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_order_item_shipped.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_soh_order_item_shipped.arn,
      ]
    }
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
      aws_sqs_queue.soh_order_item_shipped.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_order_item_shipped" {
  queue_url = aws_sqs_queue.soh_order_item_shipped.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_order_item_shipped.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_order_payment_secured" {
  statement {
    sid = "SNS-order-payment-secured"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_order_payment_secured.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_soh_order_payment_secured.arn,
      ]
    }
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
      aws_sqs_queue.soh_order_payment_secured.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_order_payment_secured" {
  queue_url = aws_sqs_queue.soh_order_payment_secured.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_order_payment_secured.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_order_item_transmitted_to_logistic" {
  statement {
    sid = "SNS-order-item-transmitted-to-logistic"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_order_item_transmitted_to_logistic.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_soh_item_transmitted_topic.arn,
      ]
    }
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
      aws_sqs_queue.soh_order_item_transmitted_to_logistic.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_order_item_transmitted_to_logistic" {
  queue_url = aws_sqs_queue.soh_order_item_transmitted_to_logistic.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_order_item_transmitted_to_logistic.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_order_item_packing_started" {
  statement {
    sid = "SNS-order-item-packing-started"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_order_item_packing_started.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_soh_packing_started_topic.arn,
      ]
    }
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
      aws_sqs_queue.soh_order_item_packing_started.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_order_item_packing_started" {
  queue_url = aws_sqs_queue.soh_order_item_packing_started.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_order_item_packing_started.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_order_tracking_id_received" {
  statement {
    sid = "SNS-order-item-tracking-id-received"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_order_item_tracking_id_received.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_soh_tracking_id_received_topic.arn,
      ]
    }
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
      aws_sqs_queue.soh_order_item_tracking_id_received.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_order_item_tracking_id_received" {
  queue_url = aws_sqs_queue.soh_order_item_tracking_id_received.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_order_tracking_id_received.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_order_item_tour_started" {
  statement {
    sid = "SNS-order-item-tour-started"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_order_item_tour_started.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_soh_order_item_tour_started.arn,
      ]
    }
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
      aws_sqs_queue.soh_order_item_tour_started.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_order_item_tour_started" {
  queue_url = aws_sqs_queue.soh_order_item_tour_started.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_order_item_tour_started.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_invoices_from_core" {

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
      aws_sqs_queue.soh_invoices_from_core.arn
    ]
  }

  statement {
    sid = "SNS-invoices-from-core"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_invoices_from_core.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        var.invoices_from_core_sns
      ]
    }
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_invoices_from_core" {
  queue_url = aws_sqs_queue.soh_invoices_from_core.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_invoices_from_core.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_core_cancellation" {

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
      aws_sqs_queue.soh_core_cancellation.arn
    ]
  }

  statement {
    sid = "SNS-core-cancellation"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_core_cancellation.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_core_cancellation_delivery_note_printed_v1.arn
      ]
    }
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_core_cancellation" {
  queue_url = aws_sqs_queue.soh_core_cancellation.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_core_cancellation.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_subsequent_delivery_received" {
  statement {
    sid = "SNS-subsequent-delivery-received"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_subsequent_delivery_received.arn
    ]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        data.aws_sns_topic.sns_core_subsequent_delivery_note_printed.arn,
      ]
    }
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
      aws_sqs_queue.soh_subsequent_delivery_received.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_subsequent_delivery_received" {
  queue_url = aws_sqs_queue.soh_subsequent_delivery_received.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_subsequent_delivery_received.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_d365_order_payment_secured" {
  statement {
    sid = "SNS-d365-order-payment-secured"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.d365_order_payment_secured.arn
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
      aws_sqs_queue.d365_order_payment_secured.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_d365_order_payment_secured" {
  queue_url = aws_sqs_queue.d365_order_payment_secured.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_d365_order_payment_secured.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_dropshipment_shipment_confirmed" {
  statement {
    sid = "SNS-dropshipment-shipment-confirmed"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_dropshipment_shipment_confirmed.arn
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
      aws_sqs_queue.soh_dropshipment_shipment_confirmed.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_dropshipment_shipment_confirmed" {
  queue_url = aws_sqs_queue.soh_dropshipment_shipment_confirmed.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_dropshipment_shipment_confirmed.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_core_sales_credit_note_created" {
  statement {
    sid = "SNS-core-sales-credit-note-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_core_sales_credit_note_created.arn
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
      aws_sqs_queue.soh_core_sales_credit_note_created.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_core_sales_credit_note_created" {
  queue_url = aws_sqs_queue.soh_core_sales_credit_note_created.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_core_sales_credit_note_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_dropshipment_purchase_order_booked" {
  statement {
    sid = "SNS-dropshipment-purchase-order-booked"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.dropshipment_purchase_order_booked.arn
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
      aws_sqs_queue.dropshipment_purchase_order_booked.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_dropshipment_purchase_order_booked" {
  queue_url = aws_sqs_queue.dropshipment_purchase_order_booked.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_dropshipment_purchase_order_booked.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_dropshipment_purchase_order_return_confirmed" {
  statement {
    sid = "SNS-dropshipment-purchase-order-return-confirmed"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.dropshipment_purchase_order_return_confirmed.arn
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
      aws_sqs_queue.dropshipment_purchase_order_return_confirmed.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_dropshipment_purchase_order_return_confirmed" {
  queue_url = aws_sqs_queue.dropshipment_purchase_order_return_confirmed.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_dropshipment_purchase_order_return_confirmed.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_dropshipment_purchase_order_return_notified" {
  statement {
    sid = "SNS-dropshipment-purchase-order-return-notified"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.arn
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
      aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_dropshipment_purchase_order_return_notified" {
  queue_url = aws_sqs_queue.soh_dropshipment_purchase_order_return_notified.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_dropshipment_purchase_order_return_notified.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_core_sales_invoice_created" {
  statement {
    sid = "SNS-soh-core-sales-invoice-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_core_sales_invoice_created.arn
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
      aws_sqs_queue.soh_core_sales_invoice_created.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_soh_core_sales_invoice_created" {
  queue_url = aws_sqs_queue.soh_core_sales_invoice_created.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_core_sales_invoice_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_migration_sales_order_created" {
  statement {
    sid = "SNS-migration-sales-order-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_migration_core_sales_order_created.arn
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
      aws_sqs_queue.soh_migration_core_sales_order_created.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_migration_sales_order_created" {
  queue_url = aws_sqs_queue.soh_migration_core_sales_order_created.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_migration_sales_order_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_migration_core_sales_invoice_created" {
  statement {
    sid = "SNS-migration-core-sales-invoice-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_migration_core_sales_invoice_created.arn
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
      aws_sqs_queue.soh_migration_core_sales_invoice_created.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_soh_migration_core_sales_invoice_created" {
  queue_url = aws_sqs_queue.soh_migration_core_sales_invoice_created.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_migration_core_sales_invoice_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_migration_core_sales_credit_note_created" {
  statement {
    sid = "SNS-migration-core-sales-credit-note-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_migration_core_sales_credit_note_created.arn
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
      aws_sqs_queue.soh_migration_core_sales_credit_note_created.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_soh_migration_core_sales_credit_note_created" {
  queue_url = aws_sqs_queue.soh_migration_core_sales_credit_note_created.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_migration_core_sales_credit_note_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_parcel_shipped" {

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
      aws_sqs_queue.soh_parcel_shipped.arn
    ]
  }

  statement {
    sid = "SNS-parce-shipped"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_parcel_shipped.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_parcel_shipped" {
  queue_url = aws_sqs_queue.soh_parcel_shipped.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_parcel_shipped.json
}

#tmp migration policies
data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_tmp_sales_order_created" {
  statement {
    sid = "SNS-tmp-sales-order-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_tmp_core_sales_order_created_dlq.arn
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
      aws_sqs_queue.soh_tmp_core_sales_order_created_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_tmp_sales_order_created" {
  queue_url = aws_sqs_queue.soh_tmp_core_sales_order_created_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_tmp_sales_order_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_tmp_core_sales_invoice_created" {
  statement {
    sid = "SNS-tmp-core-sales-invoice-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_tmp_core_sales_invoice_created_dlq.arn
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
      aws_sqs_queue.soh_tmp_core_sales_invoice_created_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_soh_tmp_core_sales_invoice_created" {
  queue_url = aws_sqs_queue.soh_tmp_core_sales_invoice_created_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_tmp_core_sales_invoice_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_tmp_core_sales_credit_note_created" {
  statement {
    sid = "SNS-tmp-core-sales-credit-note-created"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_tmp_core_sales_credit_note_created_dlq.arn
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
      aws_sqs_queue.soh_tmp_core_sales_credit_note_created_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_soh_tmp_core_sales_credit_note_created" {
  queue_url = aws_sqs_queue.soh_tmp_core_sales_credit_note_created_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_tmp_core_sales_credit_note_created.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_core_sales_invoice_created_dlq" {
  statement {
    sid = "SNS-soh-core-sales-invoice-created_dlq"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_core_sales_invoice_created_dlq.arn
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
      aws_sqs_queue.soh_core_sales_invoice_created_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_soh_core_sales_invoice_created_dlq" {
  queue_url = aws_sqs_queue.soh_core_sales_invoice_created_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_core_sales_invoice_created_dlq.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_parcel_shipped_dlq" {
  statement {
    sid = "SNS-soh-parcel-shipped_dlq"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_parcel_shipped_dlq.arn
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
      aws_sqs_queue.soh_parcel_shipped_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_soh_parcel_shipped_dlq" {
  queue_url = aws_sqs_queue.soh_parcel_shipped_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_parcel_shipped_dlq.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_ecp_shop_orders_dlq" {
  statement {
    sid = "SNS-ecp-shop-orders-dlq"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.ecp_shop_orders_dlq.arn
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
      aws_sqs_queue.ecp_shop_orders_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_ecp_order_dlq" {
  queue_url = aws_sqs_queue.ecp_shop_orders_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_ecp_shop_orders_dlq.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_bc_shop_orders_dlq" {
  statement {
    sid = "SNS-bc-shop-orders-dlq"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.bc_shop_orders_dlq.arn
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
      aws_sqs_queue.bc_shop_orders_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_bc_order_dlq" {
  queue_url = aws_sqs_queue.bc_shop_orders_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_bc_shop_orders_dlq.json
}

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_soh_core_shop_orders_dlq" {
  statement {
    sid = "SNS-core-shop-orders-dlq"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.core_shop_orders_dlq.arn
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
      aws_sqs_queue.core_shop_orders_dlq.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_core_order_dlq" {
  queue_url = aws_sqs_queue.core_shop_orders_dlq.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_core_shop_orders_dlq.json
}
