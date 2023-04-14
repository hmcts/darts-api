provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "outbound" {
  name     = "outbound-resources"
  location = "West Europe"
}

resource "azurerm_storage_account" "outbound_data_store_acc" {
  name                     = "outboundStorageAccount"
  resource_group_name      = azurerm_resource_group.example.name
  location                 = azurerm_resource_group.example.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_storage_container" "outbound_data_store_container" {
  name                  = "outboundStorageContainer"
  storage_account_name  = azurerm_storage_account.example.name
  container_access_type = "private"
}

resource "azurerm_storage_blob" "outbound_data_store_blob" {
  name                   = "outboundStorageBlob.zip"
  storage_account_name   = azurerm_storage_account.example.name
  storage_container_name = azurerm_storage_container.example.name
  type                   = "Block"
  source                 = "some-local-file.zip"
}

resource "azurerm_resource_group" "unstructured" {
  name     = "unstructured-resources"
  location = "West Europe"
}

resource "azurerm_storage_account" "unstructured_data_store_acc" {
  name                     = "unstructuredStorageAccount"
  resource_group_name      = azurerm_resource_group.example.name
  location                 = azurerm_resource_group.example.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_storage_container" "unstructured_data_store_container" {
  name                  = "unstructuredStorageContainer"
  storage_account_name  = azurerm_storage_account.example.name
  container_access_type = "private"
}

resource "azurerm_storage_blob" "unstructured_data_store_blob" {
  name                   = "unstructuredStorageBlob.zip"
  storage_account_name   = azurerm_storage_account.example.name
  storage_container_name = azurerm_storage_container.example.name
  type                   = "Block"
  source                 = "some-local-file.zip"
}