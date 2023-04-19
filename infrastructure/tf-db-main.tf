locals {
  db_name         = replace(var.component, "-", "")
  postgresql_user = "${local.db_name}_user"
}

module "database" {
  source             = "git@github.com:hmcts/terraform-module-postgresql-flexible"
  product            = var.product
  component          = var.component
  subnet_id          = data.azurerm_subnet.iaas.id
  location           = var.location
  env                = local.env_long_name
  postgresql_user    = local.postgresql_user
  database_name      = local.db_name
  common_tags        = var.common_tags
  subscription       = local.env_long_name
  business_area      = "SDS"
  postgresql_version = 11

  key_vault_rg   = "genesis-rg"
  key_vault_name = "dtssharedservices${var.env}kv"

  sku_name = var.env == "stg" || var.env == "prod" || var.env == "test" ? "GP_Gen5_8" : "GP_Gen5_2"

}

module "postgresql_role" {
  name                = data.azurerm_key_vault_secret.sdp-user.value
  login               = true
  password            = data.azurerm_key_vault_secret.sdp-pass.value
  skip_reassign_owned = true
  skip_drop_role      = true
}

module "postgresql_grant" {
  database    = module.database.postgresql_database
  role        = data.azurerm_key_vault_secret.sdp-user.value
  schema      = "public"
  object_type = "table"
  privileges  = ["SELECT"]
  objects     = ["sdp_mat_view_location", "sdp_mat_view_artefact"]
}
