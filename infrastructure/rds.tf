module "aurora" {
  source                          = "git@github.com:kfzteile24/bop-terraform-module-aurora.git?ref=v1.0.0"
  name                            = "${local.service}-aurora-serverless"
  engine                          = "aurora-postgresql"
  engine_version                  = "11.13"
  replica_scale_enabled           = false
  replica_count                   = var.db_instance_scale
  backtrack_window                = 10 # ignored in serverless
  database_name                   = "sales_order_hub"
  subnets                         = module.common.db_subnet_ids
  vpc_id                          = module.common.vpc_id
  vpc_security_group_ids          = [module.rds-security-group.this_security_group_id, module.common.bastion_to_rds_sg_id]
  monitoring_interval             = 60
  instance_type                   = var.db_instance_class
  apply_immediately               = true
  skip_final_snapshot             = true
  storage_encrypted               = true
  auto_minor_version_upgrade      = false
  performance_insights_enabled    = var.db_performance_insight
}
