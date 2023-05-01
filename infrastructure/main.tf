provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}

# locals {

#   shared_infra_rg           = "${var.product}-shared-infrastructure-${var.env}"
#   # vault_name                = "${var.product}-kv-${var.env}"
# }
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-shared-${var.env}"
  location = var.location

  tags = var.common_tags
}

data "azurerm_subnet" "postgres" {
  name                 = "iaas"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = "ss-${var.env}-vnet"
}

module "key-vault" {
  source              = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  name                = "darts-kv-${var.env}"
  product             = var.product
  env                 = var.env
  tenant_id           = var.tenant_id
  object_id           = var.jenkins_AAD_objectId
  resource_group_name = azurerm_resource_group.rg.name
  product_group_name  = "DTS Darts Modernisation"
  common_tags         = var.common_tags
  create_managed_identity    = true
}


resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "darts-api-POSTGRES-USER"
  value        = module.postgresql_flexible.username
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "darts-api-POSTGRES-PASS"
  value        = module.postgresql_flexible.password
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "darts-api-POSTGRES-HOST"
  value        = module.postgresql_flexible.fqdn
  key_vault_id = module.key-vault.key_vault_id
}

module "postgresql_flexible" {
    providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source        = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env           = var.env
  product       = var.product
  name          = "${var.product}-v14-flexible"
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

