resource "aws_ses_email_identity" "soh_source_email" {
  email = aws_ssm_parameter.soh_source_email.value
}
