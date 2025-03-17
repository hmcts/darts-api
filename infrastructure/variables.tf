variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {
}

variable "aks_subscription_id" {
}

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "common_tags" {
  type = map(string)
}

variable "businessArea" {
  default = "sds"
}

variable "account_kind" {
  description = "Defines the Kind of account. Valid options are Storage, StorageV2 and BlobStorage. Changing this forces a new resource to be created."
  default     = "StorageV2"
}

variable "pgsqlSku" {
  description = "sku set for postgreSQL databse"
  default     = "GP_Standard_D4ds_v5"
}

variable "pgsqlstoragemb" {
  description = "Max storage allowed for the PGSql Flexibile instance"
  type        = number
  default     = 165536
}
