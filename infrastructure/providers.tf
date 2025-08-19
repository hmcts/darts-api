terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.34"
    }
    random = {
      source = "hashicorp/random"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.4.0"
    }
    azapi = {
      source  = "Azure/azapi"
      version = "~> 2.5.0"
    }
  }
}
