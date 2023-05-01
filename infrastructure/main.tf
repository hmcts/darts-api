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
  default_name           = var.component != "" ? "${var.product}-${var.component}" : var.product
  name                   = var.name != "" ? var.name : local.default_name
  server_name            = "${local.name}-${var.env}"
  postgresql_rg_name     = "darts-rg"
  postgresql_rg_location = "UK-South"
  vnet_rg_name           = var.business_area == "sds" ? "ss-${var.env}-network-rg" : "core-infra-${var.env}"
  vnet_name              = var.business_area == "sds" ? "ss-${var.env}-vnet" : "core-infra-vnet-${var.env}"

  private_dns_zone_id = "/subscriptions/1baf5470-1c3e-40d3-a6f7-74bfbce4b348/resourceGroups/core-infra-intsvc-rg/providers/Microsoft.Network/privateDnsZones/private.postgres.database.azure.com"

  is_prod = length(regexall(".*(prod).*", var.env)) > 0

  admin_group    = local.is_prod ? "DTS Platform Operations SC" : "DTS Platform Operations"
  db_reader_user = local.is_prod ? "DTS JIT Access ${var.product} DB Reader SC" : "DTS ${upper(var.business_area)} DB Access Reader"


  high_availability_environments = ["ptl", "perftest", "stg", "aat", "prod"]
  high_availability              = var.high_availability == true || contains(local.high_availability_environments, var.env)



}

# data "azurerm_subnet" "pg_subnet" {
#   name                 = "postgresql"
#   resource_group_name  = local.vnet_rg_name
#   virtual_network_name = local.vnet_name

#   count = var.pgsql_delegated_subnet_id == "" ? 1 : 0
# }

data "azurerm_client_config" "current" {}
 
data "azuread_group" "db_admin" {
  display_name     = local.admin_group
  security_enabled = true
}

data "azuread_service_principal" "mi_name" {
  count     = var.enable_read_only_group_access ? 1 : 0
  object_id = var.admin_user_object_id
}


resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-shared-${var.env}"
  location = var.location

  tags = var.common_tags
}

module "key-vault" {
  source              = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  name                = "darts-${var.env}"
  product             = var.product
  env                 = var.env
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

  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env    = var.env

  product       = var.product
  component     = var.component
  business_area = "sds" # sds or cft
  
  pgsql_databases = [
    {
      name : "application"
    }
  ]

  pgsql_version = "14"
  
  # The ID of the principal to be granted admin access to the database server, should be the principal running this normally
  admin_user_object_id = var.admin_user_object_id
  
  common_tags = var.common_tags
}


resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.darts-api-db.username
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.darts-api-db.password
  key_vault_id = module.key-vault.key_vault_id
}

# resource "azurerm_key_vault_secret" "POSTGRES-HOST" {
#   name         = "${var.component}-POSTGRES-HOST"
#   value        = module.darts-api-db.host_name
#   key_vault_id = module.key-vault.key_vault_id
# }

# resource "azurerm_key_vault_secret" "POSTGRES-PORT" {
#   name         = "${var.component}-POSTGRES-PORT"
#   value        = module.darts-api-db.postgresql_listen_port
#   key_vault_id = module.key-vault.key_vault_id
# }

# resource "azurerm_key_vault_secret" "POSTGRES-DATABASE" {
#   name         = "${var.component}-POSTGRES-DATABASE"
#   value        = module.darts-api-db.name
#   key_vault_id = module.key-vault.key_vault_id
# }


