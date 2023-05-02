locals {
  resource_group_name = "${var.product}-${var.env}-rg"
  key_vault_name      = "${var.product}-${var.env}"
}

data "azurerm_key_vault" "key_vault" {
  name                = local.key_vault_name
  resource_group_name = local.resource_group_name
}
