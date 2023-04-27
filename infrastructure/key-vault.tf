data "azurerm_key_vault" "civil_vault" {
  name                = "civil-shared-${var.env}"
  resource_group_name = local.civil_shared_resource_group
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}



data "azurerm_key_vault_secret" "api_gw_s2s_key" {
  name         = "microservicekey-api-gw"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}

resource "azurerm_key_vault_secret" "api_gw_s2s_secret" {
  name         = "api-gateway-s2s-secret"
  value        = data.azurerm_key_vault_secret.api_gw_s2s_key.value
  key_vault_id = data.azurerm_key_vault.civil_vault.id
}
