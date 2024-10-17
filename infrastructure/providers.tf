terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.5"
    }
    random = {
      source = "hashicorp/random"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "3.0.2"
    }
  }
}
