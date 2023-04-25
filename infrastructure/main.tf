locals {
  env = (var.env == "aat") ? "stg" : (var.env == "sandbox") ? "sbox" : "${(var.env == "perftest") ? "test" : "${var.env}"}"

  env_subdomain = local.env_long_name == "prod" ? "" : "${local.env_long_name}."
  base_url      = "${var.product}-${var.component}.${local.env_subdomain}platform.hmcts.net"

  apim_name     = "sds-api-mgmt-${local.env}"
  apim_rg       = "ss-${local.env}-network-rg"
  env_long_name = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env

  deploy_apim = local.env == "stg" || local.env == "sbox" || local.env == "prod" ? 1 : 0

  prefix            = "${var.product}-ss"
  prefix_no_special = replace(local.prefix, "-", "")
}

# Resource Group

resource "azurerm_resource_group" "rg" {
  name     = "darts-api-dev"
  location = "uksouth"
  #tags     = var.resource_group_tags
}

# Application Insights

resource "azurerm_application_insights" "appinsights" {
  name                = "darts-api-appinsights-dev"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"
  #tags                = var.common_tags

  lifecycle {
    ignore_changes = [
      # Ignore changes to appinsights as otherwise upgrading to the Azure provider 2.x
      # destroys and re-creates this appinsights instance
      application_type,
    ]
  }
}

# this key vault is created in every environment, but preview, being short-lived,
# will use the aat one instead
# This section includes hardcoded variables that were in variables.tf ...
# ... said variables were moved here due to a Jenkins objection
module "key-vault" {
  source              = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product             = "darts"
  env                 = ""
  tenant_id           = ""
  object_id           = ""
  resource_group_name = azurerm_resource_group.rg.name
  # https://github.com/hmcts/devops-azure-ad/blob/master/users/prod_users.yml
  product_group_name = "Key Vault product group name"
  #common_tags                 = ""
  managed_identity_object_ids = ["${data.azurerm_user_assigned_identity.rpe-shared-identity.principal_id}"]
}

# Key Vault secrets

resource "azurerm_key_vault_secret" "app_insights_connection_string" {
  name         = "app-insights-connection-string"
  value        = azurerm_application_insights.appinsights.connection_string
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "azure_appinsights_key" {
  name         = "AppInsightsInstrumentationKey"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
}
