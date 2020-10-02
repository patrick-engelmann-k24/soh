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

    resources = [aws_sqs_queue.ecp_shop_orders.arn]

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

    resources = [aws_sqs_queue.ecp_shop_orders.arn]
  }
}

resource "aws_sqs_queue_policy" "sns_sqs_sendmessage_policy_parcel_shipped" {
  queue_url = aws_sqs_queue.ecp_shop_orders.id
  policy    = data.aws_iam_policy_document.sns_sqs_sendmessage_policy_document_soh_ecp_shop_orders.json
}