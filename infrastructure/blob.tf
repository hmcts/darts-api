locals {
  blob_folders = [
    "collected",
    "response",
    "submission"
  ]
}

data "azurerm_subnet" "private_endpoints" {
  resource_group_name  = local.private_endpoint_rg_name
  virtual_network_name = local.private_endpoint_vnet_name
  name                 = "private-endpoints"
}

module "armsa" {
  source                           = "git@github.com:hmcts/cnp-module-storage-account?ref=4.x"
  env                              = var.env
  storage_account_name             = "${var.product}arm${var.env}"
  resource_group_name              = local.rg_name
  location                         = var.location
  account_kind                     = var.account_kind
  enable_hns                       = true
  account_replication_type         = "ZRS"
  common_tags                      = var.common_tags
  cross_tenant_replication_enabled = true
  private_endpoint_subnet_id       = data.azurerm_subnet.private_endpoints.id
  default_action                   = "Allow"
  containers = [{
    name        = "dropzone"
    access_type = "private"
  }]
}

resource "azurerm_storage_blob" "blob_folder" {
  for_each               = toset(local.blob_folders)
  name                   = "${each.value}/"
  storage_account_name   = module.armsa.storageaccount_name
  storage_container_name = "dropzone"
  type                   = "Block"
}

resource "azurerm_role_assignment" "storage_contributors" {
  for_each             = toset(var.storage_account_contributor_ids)
  scope                = module.armsa.storageaccount_id
  role_definition_name = "Storage Account Contributor"
  principal_id         = each.value
}

