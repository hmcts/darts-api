terraform {
  required_version = ">= 1.0.4"
  required_providers {
    azurerm = {
      version = "3.41.0"
    }
  }
}

provider "azurerm" {
  features {}
}
backend "azurerm" {}

required_providers {
  azurerm = {
    source  = "hashicorp/azurerm"
    version = "3.54"
  }
  random = {
    source = "hashicorp/random"
  }
  azuread = {
    source  = "hashicorp/azuread"
    version = "2.38.0"
  }
}