provider "azurerm" {
  features {}
}

locals {
  prefix               = "${var.product}-ss"
  prefix_no_special    = replace(local.prefix, "-", "")
  resource_group_name  = "${local.prefix}-${var.env}-rg"
  storage_account_name_unstructured = "dartsStorageAccU"
  storage_container_name_unstructured = "dartsStorageContU"
  storage_blob_name_unstructured = "unstructuredBlob"
  storage_account_name_outbound = "dartsStorageAccO"
  storage_container_name_outbound = "dartsStorageContO"
  storage_blob_name_outbound = "outboundBlob"
  key_vault_name       = "unstructuredKV"
  env_long_name        = var.env == "sbox" ? "sandbox" : var.env == "stg" ? "staging" : var.env

  frontend_url = "${var.env == "prod" ? "www" : "darts-api"}.${var.domain}"

  secret_expiry = "2024-03-01T01:00:00Z"
}
# resource "azurerm_resource_group" "outbound" {
#   name                   = local.resource_group_name
#   location               = variable.location
# }

# resource "azurerm_storage_account" "outbound_data_store_acc" {
#   name                     = local.storage_account_name
#   resource_group_name      = local.resource_group_name
#   location                 = variable.location
#   account_tier             = "Standard"
#   account_replication_type = "LRS"
# }

# resource "azurerm_storage_container" "outbound_data_store_container" {
#   name                   = local.storage_container_name
#   storage_account_name   = local.storage_account_name
#   container_access_type  = "private"
# }

module "outbound_data_store_blob" {
  source =  "git@ghttps://github.com/hmcts/chart-blobstorage?ref=master"
  name                   = local.storage_blob_name
  storage_account_name   = local.storage_account_name
  storage_container_name = local.storage_container_name
  type                   = "Block"
  # source                 = value.path
}

# resource "azurerm_resource_group" "unstructured" {
#   name                   = local.resource_group_name
#   location               = variable.location
# }

# resource "azurerm_storage_account" "unstructured_data_store_acc" {
#   name                     = local.storage_account_name
#   resource_group_name      = local.resource_group_name
#   location                 = variable.location
#   account_tier             = "Standard"
#   account_replication_type = "LRS"
# }

# resource "azurerm_storage_container" "unstructured_data_store_container" {
#   source = 
#   name                   = local.storage_container_name
#   storage_account_name  = local.storage_account_name
#   container_access_type = "private"
# }

module "unstructured_data_store_blob" {
  source =  "git@ghttps://github.com/hmcts/chart-blobstorage?ref=master"
  name_unstructured                   = local.storage_blob_name
  storage_account_name_unstructured   = local.storage_account_name
  storage_container_name_unstructured = local.storage_container_name
  type                   = "Block"
  # source                 = value.path
}