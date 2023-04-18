provider "azurerm" {
  features {}
}

locals {
  prefix               = "${var.product}-ss"
  prefix_no_special    = replace(local.prefix, "-", "")
  resource_group_name  = "${local.prefix}-${var.env}-rg"
  storage_account_name = "${local.prefix_no_special}sa${var.env}"
  storage_container_name = "${local.prefix_no_special}sc${var.env}"
  storage_blob_name = "${local.prefix_no_special}sb${var.env}"
  key_vault_name       = "${local.prefix}-kv-${var.env}"
  env_long_name        = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env

  frontend_url = "${var.env == "prod" ? "www" : "darts-api"}.${var.domain}"

  secret_expiry = "2024-03-01T01:00:00Z"
}
resource "azurerm_resource_group" "outbound" {
  name                   = local.resource_group_name
  location               = "West Europe"
}

resource "azurerm_storage_account" "outbound_data_store_acc" {
  name                     = local.storage_account_name
  resource_group_name      = local.resource_group_name
  location                 = "West Europe"
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

# resource "azurerm_storage_container" "outbound_data_store_container" {
#   name                   = local.storage_container_name
#   storage_account_name   = local.storage_account_name
#   container_access_type  = "private"
# }

resource "azurerm_storage_blob" "outbound_data_store_blob" {
  name                   = local.storage_blob_name
  storage_account_name   = local.storage_account_name
  # storage_container_name = local.storage_container_name
  type                   = "Block"
  source_content         = each.value.content
}

resource "azurerm_resource_group" "unstructured" {
  name                   = local.resource_group_name
  location               = "West Europe"
}

resource "azurerm_storage_account" "unstructured_data_store_acc" {
  name                     = local.storage_account_name
  resource_group_name      = local.resource_group_name
  location                 = "West Europe"
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

# resource "azurerm_storage_container" "unstructured_data_store_container" {
#   name                   = local.storage_container_name
#   storage_account_name  = local.storage_account_name
#   container_access_type = "private"
# }

resource "azurerm_storage_blob" "unstructured_data_store_blob" {
  name                   = local.storage_blob_name
  storage_account_name   = local.storage_account_name
  # storage_container_name = local.storage_container_name
  type                   = "Block"
  source_content         = each.value.content
}