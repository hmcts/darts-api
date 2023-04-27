module "servicebus-namespace" {
  providers = {
    azurerm.private_endpoint = azurerm.private_endpoint
  }
  source              = "git@github.com:hmcts/terraform-module-servicebus-namespace?ref=master"
  name                = "${var.product}-servicebus-${var.env}"
  resource_group_name = local.civil_shared_resource_group
  location            = var.location
  env                 = var.env
  common_tags         = local.tags
  sku                 = var.sku
  zone_redundant      = (var.sku != "Premium" ? "false" : "true")
}

module "servicebus-sdt-queue" {
  source              = "git@github.com:hmcts/terraform-module-servicebus-queue?ref=master"
  name                = "${var.product}-${var.component}-in-out-${var.env}"
  namespace_name      = module.servicebus-namespace.name
  resource_group_name = local.civil_shared_resource_group

  depends_on = [module.servicebus-namespace]
}

resource "azurerm_key_vault_secret" "servicebus_primary_connection_string" {
  name         = "civil-sdt-servicebus-connection-string"
  value        = module.servicebus-namespace.primary_send_and_listen_connection_string
  key_vault_id = data.azurerm_key_vault.civil_vault.id
}

resource "azurerm_key_vault_secret" "servicebus_primary_shared_access_key" {
  name         = "civil-sdt-servicebus-shared-access-key"
  value        = module.servicebus-namespace.primary_send_and_listen_shared_access_key
  key_vault_id = data.azurerm_key_vault.civil_vault.id
}
