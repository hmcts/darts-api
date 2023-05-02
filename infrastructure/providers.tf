terraform {
  required_version = ">= 1.0.4"
  required_providers {
    azurerm = {
      version = "3.41.0"
    }
    postgresql = {
      source  = "cyrilgdn/postgresql"
      version = ">=1.17.1"
    }
  }
}

provider "azurerm" {
  features {}
}