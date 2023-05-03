variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {
 }

 variable aks_subscription_id {
 }

variable "common_tags" {
  type = map(string)
}