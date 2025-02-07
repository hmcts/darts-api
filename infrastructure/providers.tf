terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.18"
    }
    random = {
      source = "hashicorp/random"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.1.0"
    }
    azapi = {
      source  = "Azure/azapi"
      version = "~> 2.2.0"
    }
  }
}
