# Resource Group variables

variable "resource_group_name" {
  default = "${var.product}-${var.component}-${var.env}"
  description = "Name of the resource group"
}

variable "resource_group_location" {
  default = "uksouth"
  description = "Location of the resource group"
}

# Application Insights variables

variable "appinsights_name" {
  default = "${var.product}-${var.component}-appinsights-${var.env}"
  description = "Name of Application Insights"
}

variable "appinsights_application_type" {
  default = "web"
  description = "Application Insights application type"
}

# Azure user assigned identity variables

variable "azurerm_user_assigned_identity_name" {
  default = "pre-${var.env}-mi"
  description = "Azure user assigned identity name"
}

variable "azurerm_user_assigned_identity_rg_name" {
  default = "managed-identities-${var.env}-rg"
  description = "Azure user assigned identity resource group name"
}

# Key Vault variables

variable "key_vault_source" {
  default = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  description = "Key Vault source"
}

variable "key_vault_product_group_name" {
  # https://github.com/hmcts/devops-azure-ad/blob/master/users/prod_users.yml
  default = "DTS Darts Modernisation"
  description = "Key Vault product group name"
}

# Key Vault secret variables

variable "azurerm_key_vault_secret_conn_str" {
  default = "app-insights-connection-string"
  description = "Key Vault connection string name"
}

variable "azurerm_key_vault_secret_insights_key" {
  default = "AppInsightsInstrumentationKey"
  description = "Key Vault app insights key name"
}

# These variables are require by a Jenkins shell script used during deployment
# They are not necessarily used by the resources within this Terraform plan

variable "product" {}

variable "component" {}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}