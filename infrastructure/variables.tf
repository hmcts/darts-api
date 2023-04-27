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

variable "jenkins_AAD_objectId" {
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}