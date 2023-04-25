variable "product" {
  default = "darts"
}

variable "component" {
  default = "api"
}

variable "location" {
  default = "UK South"
}
variable "env" {}

variable "subscription" {
  default = ""
}

variable "deployment_namespace" {
  default = ""
}

variable "common_tags" {
  type = map(string)
}
/*
# Resource Group variables

variable "resource_group_name" {
  default     = "${var.product}-${var.component}-${var.env}"
  description = "Name of the resource group"
}

variable "resource_group_location" {
  default     = "uksouth"
  description = "Location of the resource group"
}

# Application Insights variables

variable "appinsights_name" {
  default     = "${var.product}-${var.component}-appinsights-${var.env}"
  description = "Name of Application Insights"
}

variable "appinsights_application_type" {
  default     = "web"
  description = "Application Insights application type"
}
*/
# Azure user assigned identity variables

variable "azurerm_user_assigned_identity_name" {
  default     = "pre-${var.env}-mi"
  description = "Azure user assigned identity name"
}

variable "azurerm_user_assigned_identity_rg_name" {
  default     = "managed-identities-${var.env}-rg"
  description = "Azure user assigned identity resource group name"
}
/*
# Key Vault secret variables

variable "azurerm_key_vault_secret_conn_str" {
  default     = "app-insights-connection-string"
  description = "Key Vault connection string name"
}

variable "azurerm_key_vault_secret_insights_key" {
  default     = "AppInsightsInstrumentationKey"
  description = "Key Vault app insights key name"
}
*/