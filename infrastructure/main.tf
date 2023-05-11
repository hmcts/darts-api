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
  vault_name                = "${var.product}-${var.env}"
  rg_name                   = "${var.product}-${var.env}-rg"
}

 data "azurerm_resource_group" "rg" {
   name     = local.rg_name
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


 resource "azurerm_key_vault_secret" "POSTGRES-USER" {
   name         = "darts-api-POSTGRES-USER"
   value        = module.postgresql_flexible.username
   key_vault_id = data.azurerm_key_vault.key_vault.id
 }

 resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
   name         = "darts-api-POSTGRES-PASS"
   value        = module.postgresql_flexible.password
   key_vault_id = data.azurerm_key_vault.key_vault.id
 }

 resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
   name         = "darts-api-POSTGRES-HOST"
   value        = module.postgresql_flexible.fqdn
   key_vault_id = data.azurerm_key_vault.key_vault.id
 }


 module "postgresql_flexible" {
     providers = {
     azurerm.postgres_network = azurerm.postgres_network
   }

   source        = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
   env           = var.env
   product       = var.product
   resource_group_name = local.rg_name
   component     = var.component
   business_area = "sds"
   location      = var.location

   common_tags = var.common_tags
   admin_user_object_id = var.jenkins_AAD_objectId
   pgsql_databases = [
     {
       name : "darts"
     }
   ]

   pgsql_version = "14"
 }