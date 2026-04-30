terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.62"
    }
    random = {
      source = "hashicorp/random"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.8.0"
    }
    azapi = {
      source  = "Azure/azapi"
      version = "~> 2.9.0"
    }
  }
}
