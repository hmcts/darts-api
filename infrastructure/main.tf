provider "azurerm" {
  features {}
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}

locals {
  vault_name                 = "${var.product}-${var.env}"
  rg_name                    = "${var.product}-${var.env}-rg"
  db_name                    = "darts"
  db_port                    = 5432
  private_endpoint_rg_name   = var.businessArea == "sds" ? "ss-${var.env}-network-rg" : "${var.businessArea}-${var.env}-network-rg"
  private_endpoint_vnet_name = var.businessArea == "sds" ? "ss-${var.env}-vnet" : "${var.businessArea}-${var.env}-vnet"
}

data "azurerm_resource_group" "rg" {
  name = local.rg_name
}

data "azurerm_subnet" "postgres" {
  name                 = "iaas"
  resource_group_name  = "ss-${var.env}-network-rg"
  virtual_network_name = "ss-${var.env}-vnet"
}

data "azurerm_key_vault" "key_vault" {
  name                = local.vault_name
  resource_group_name = local.rg_name
}

data "azurerm_client_config" "current" {}

data "azuread_group" "jit_admin_group" {
  display_name     = "DTS JIT Access Darts DB Admin"
  security_enabled = true
}

