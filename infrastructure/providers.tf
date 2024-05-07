terraform {
   backend "azurerm" {}

   required_providers {
     azurerm = {
       source  = "hashicorp/azurerm"
       version = "3.101"
     }
     random = {
       source = "hashicorp/random"
     }
     azuread = {
       source  = "hashicorp/azuread"
       version = "2.48.0"
     }
   }
 }
