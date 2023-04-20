# Resource Group

resource "azurerm_resource_group" "rg" {
  name = var.resource_group_name
  location = var.resource_group_location
  tags = var.resource_group_tags
}

# Application Insights

resource "azurerm_application_insights" "appinsights" {
  name = var.appinsights_name
  location = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  application_type = var.appinsights_application_type
  tags = var.common_tags

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
  source = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product = var.product
  env = var.env
  tenant_id = var.tenant_id
  object_id = var.jenkins_AAD_objectId
  resource_group_name = azurerm_resource_group.rg.name
  # https://github.com/hmcts/devops-azure-ad/blob/master/users/prod_users.yml
  product_group_name  = "Key Vault product group name"
  common_tags = var.common_tags
  managed_identity_object_ids = ["${data.azurerm_user_assigned_identity.rpe-shared-identity.principal_id}"]
}

# Key Vault secrets

resource "azurerm_key_vault_secret" "app_insights_connection_string" {
  name = var.azurerm_key_vault_secret_conn_str
  value = azurerm_application_insights.appinsights.connection_string
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "AZURE_APPINSGHTS_KEY" {
  name = var.azurerm_key_vault_secret_insights_key
  value = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
}
