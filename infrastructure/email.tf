resource "aws_ses_email_identity" "soh_source_email" {
  email = aws_ssm_parameter.soh_source_email.value
}

resource "aws_ses_email_identity" "finance_destination_email" {
  email = aws_ssm_parameter.finance_destination_email.value
}

resource "aws_ses_email_identity" "refund_cc_email" {
  email = aws_ssm_parameter.refund_cc_email.value
}