output "vaultName" {
  value = data.azurerm_key_vault.civil_vault.name
}

output "vaultUri" {
  value = data.azurerm_key_vault.civil_vault.vault_uri
}

output "sb_primary_send_and_listen_connection_string" {
  value     = module.servicebus-namespace.primary_send_and_listen_connection_string
  sensitive = true
}

output "sb_primary_send_and_listen_shared_access_key" {
  value     = module.servicebus-namespace.primary_send_and_listen_shared_access_key
  sensitive = true
}
