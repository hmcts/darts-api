variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {
  default = "demo"
}

variable "aks_subscription_id" {
  default = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}
