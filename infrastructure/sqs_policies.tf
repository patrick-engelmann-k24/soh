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

data "aws_iam_policy_document" "sns_sqs_sendmessage_policy_document_core_return_delivery_note_printed" {
  statement {
    sid = "SNS-core-return-deliver-note-printed"
    effect = "Allow"

    actions = [
      "sqs:SendMessage",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      aws_sqs_queue.soh_core_return_delivery_note_printed.arn
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
      aws_sqs_queue.soh_core_return_delivery_note_printed.arn
    ]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_core_return_delivery_note_printed" {
  queue_url = aws_sqs_queue.soh_core_return_delivery_note_printed.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_core_return_delivery_note_printed.json
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
