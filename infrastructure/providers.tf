terraform {
   backend "azurerm" {}

   required_providers {
     azurerm = {
       source  = "hashicorp/azurerm"
       version = "3.58"
     }
     random = {
       source = "hashicorp/random"
     }
     azuread = {
       source  = "hashicorp/azuread"
       version = "2.39.0"
     }
   }
 }