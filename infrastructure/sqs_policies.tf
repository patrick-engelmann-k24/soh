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

//    condition {
//      test     = "ArnEquals"
//      variable = "aws:SourceArn"
//      values = [
//        "arn:aws:sns:eu-central-1:726569450381:development-order-export",
//      ]
//    }
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
