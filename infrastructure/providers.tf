terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.9"
    }
    random = {
      source = "hashicorp/random"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.0.2"
    }
    azapi = {
      source  = "Azure/azapi"
      version = "~> 2.0.0"
    }
  }
}
