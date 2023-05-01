variable "product" {
  default = "darts"
}

variable "component" {
  default = "api"
}


variable "location" {
  type    = string
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "team_contact" {
  type        = string
  description = "The name of your Slack channel people can use to contact your team about your infrastructure"
  default     = "#darts-devs"
}

variable "destroy_me" {
  type        = string
  description = "Here be dragons! In the future if this is set to Yes then automation will delete this resource on a schedule. Please set to No unless you know what you are doing"
  default     = "No"
}

variable "sku" {
  type        = string
  default     = "Premium"
  description = "SKU type(Basic, Standard and Premium)"
}

variable "tenant_id" {
  type        = string
  description = "(Required) The Azure Active Directory tenant ID that should be used for authenticating requests to the key vault. This is usually sourced from environment variables and not normally required to be specified."
}

variable "jenkins_AAD_objectId" {
  type        = string
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}


variable "sku_name" {
  default = "GP_Gen5_2"
}

variable "sku_tier" {
  default = "GeneralPurpose"
}

variable "storage_mb" {
  default = "51200"
}

variable "sku_capacity" {
  default = "2"
}

variable "ssl_enforcement" {
  default = "Enabled"
}

variable "backup_retention_days" {
  default = "35"
}

variable "georedundant_backup" {
  default = "Enabled"
}

// Define the values for mandatory/required parameters (see https://github.com/hmcts/cnp-module-postgres)

variable "postgresql_user" {
  default = "darts"
}

variable "database_name" {
  default = "darts-api-db"
}

variable "postgresql_version" {
  default = "11"
}
variable "aks_subscription_id" {}

variable "admin_user_object_id" {
  default = null
}

variable "name" {
  default = "darts"
}
variable "resource_group_name" {
  default = "darts-rg"
}

variable "business_area"{
  default="sds"
}
variable "high_availability" {
  default= "false"
  
}
