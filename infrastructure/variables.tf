variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {
  default = "demo"
}

variable "aks_subscription_id" {
  default = "${TF_VAR_aks_subscription_id}"
}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}
