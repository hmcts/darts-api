provider "azurerm" {
  features {}
}

locals {
  app_full_name = "${var.product}-${var.component}"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-shared-${var.env}"
  location = var.location

  tags = var.common_tags
}

module "key-vault" {
  source              = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product             = var.product
  env                 = var.env
  tenant_id           = var.tenant_id
  object_id           = var.jenkins_AAD_objectId
  resource_group_name = azurerm_resource_group.rg.name
  product_group_name  = "DTS Darts Modernisation"
  common_tags         = var.common_tags
  create_managed_identity    = true
}

resource "azurerm_key_vault_secret" "AZURE_APPINSGHTS_KEY" {
  name         = "AppInsightsInstrumentationKey"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.env}"
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"
  tags                = var.common_tags
}

module "darts-api-db" {
  source                = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product               = var.product
  component             = var.component
  name                  = "${local.app_full_name}-postgres-db"
  location              = var.location
  env                   = var.env
  subscription          = var.subscription
  postgresql_user       = var.postgresql_user
  postgresql_version    = var.postgresql_version
  database_name         = var.database_name
  sku_name              = var.sku_name
  sku_tier              = var.sku_tier
  sku_capacity          = var.sku_capacity
  ssl_enforcement       = var.ssl_enforcement
  storage_mb            = var.storage_mb
  backup_retention_days = var.backup_retention_days
  georedundant_backup   = var.georedundant_backup
  common_tags           = var.common_tags
}


resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.darts-api-db.user_name
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.darts-api-db.postgresql_password
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.darts-api-db.host_name
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = module.darts-api-db.postgresql_listen_port
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = module.darts-api-db.postgresql_database
  key_vault_id = module.key-vault.key_vault_id
}

