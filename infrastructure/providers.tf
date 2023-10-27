terraform {
   backend "azurerm" {}

   required_providers {
     azurerm = {
       source  = "hashicorp/azurerm"
       version = "3.78"
     }
     random = {
       source = "hashicorp/random"
     }
     azuread = {
       source  = "hashicorp/azuread"
       version = "2.45.0"
     }
   }
 }