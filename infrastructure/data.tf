# Azure user assigned identity

data "azurerm_user_assigned_identity" "rpe-shared-identity" {
  name = var.azurerm_user_assigned_identity_name
  resource_group_name = var.azurerm_user_assigned_identity_rg_name
}
