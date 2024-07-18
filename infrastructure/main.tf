provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}

locals {
  vault_name = "${var.product}-${var.env}"
  rg_name    = "${var.product}-${var.env}-rg"
  db_name    = "darts"
  db_port    = 5432
  private_endpoint_rg_name   = var.businessArea == "sds" ? "ss-${var.env}-network-rg" : "${var.businessArea}-${var.env}-network-rg"
  private_endpoint_vnet_name = var.businessArea == "sds" ? "ss-${var.env}-vnet" : "${var.businessArea}-${var.env}-vnet"
}

data "azurerm_resource_group" "rg" {
  name = local.rg_name
}

data "azurerm_subnet" "postgres" {
  name                 = "iaas"
  resource_group_name  = "ss-${var.env}-network-rg"
  virtual_network_name = "ss-${var.env}-vnet"
}

data "azurerm_key_vault" "key_vault" {
  name                = local.vault_name
  resource_group_name = local.rg_name
}

resource "azurerm_key_vault_secret" "POSTGRES-CONNECTION-STRING" {
  name         = "api-POSTGRES-CONNECTION-STRING"
  value        = "postgres://${module.postgresql_flexible.username}:${module.postgresql_flexible.password}@${module.postgresql_flexible.fqdn}:${local.db_port}/${local.db_name}"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "api-POSTGRES-USER"
  value        = module.postgresql_flexible.username
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "api-POSTGRES-PASS"
  value        = module.postgresql_flexible.password
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "api-POSTGRES-HOST"
  value        = module.postgresql_flexible.fqdn
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "api-POSTGRES-PORT"
  value        = local.db_port
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "api-POSTGRES-DATABASE"
  value        = local.db_name
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

module "postgresql_flexible" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source              = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env                 = var.env
  product             = var.product
  resource_group_name = local.rg_name
  component           = var.component
  business_area       = "sds"
  location            = var.location
  storage_mb          = "262144"

  common_tags          = var.common_tags
  admin_user_object_id = var.jenkins_AAD_objectId
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "pg_stat_statements"
    }
  ]
  pgsql_version = "15"
}

data "azurerm_subnet" "private_endpoints" {
  resource_group_name  = local.private_endpoint_rg_name
  virtual_network_name = local.private_endpoint_vnet_name
  name                 = "private-endpoints"
}

module "armsa" {
  source                   = "git@github.com:hmcts/cnp-module-storage-account?ref=master"
  env                      = var.env
  storage_account_name     = "${var.product}arm${var.env}"
  resource_group_name      = local.rg_name
  location                 = var.location
  account_kind             = var.account_kind
  enable_hns               = true
  account_replication_type = "ZRS"
  common_tags          = var.common_tags

  private_endpoint_subnet_id = data.azurerm_subnet.private_endpoints.id
}
