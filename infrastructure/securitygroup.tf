module "rds-security-group" {
  source = "terraform-aws-modules/security-group/aws"
  version = "v3.4.0"

  name        = "${local.service}-rds-sg"
  description = "inbound and outbound traffic for rds"
  vpc_id      = module.common.vpc_id

  ingress_with_source_security_group_id = [
    {
      description              = "Allow all incoming Postgres traffic from ECS and the bastion"
      from_port                = 5432
      to_port                  = 5432
      protocol                 = "tcp"
      source_security_group_id = module.application_module.service_sg_id
    }
  ]

  egress_with_source_security_group_id = [
    {
      from_port                = 5432
      to_port                  = 5432
      protocol                 = "tcp"
      description              = "Allow all outgoing Postgres traffic to ECS and the bastion"
      source_security_group_id = module.application_module.service_sg_id
    }
  ]

  tags = {
    Name        = "rds-sg"
    Group       = local.service
    GitRepoName = local.service
  }
} 