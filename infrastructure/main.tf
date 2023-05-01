provider "azurerm" {
  features {}
}
provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
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
  display_name = "Darts DB"
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




