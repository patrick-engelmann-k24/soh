resource "aws_alb_listener_rule" "public" {
  listener_arn = module.application_module.main_alb_listener_arn

  action {
    type             = "forward"
    target_group_arn = module.application_module.main_alb_target_arn
  }

  condition {
    field  = "host-header"
    values = ["${local.service}.${var.domain_name}"]
  }

  lifecycle {
    create_before_destroy = true
  }
}