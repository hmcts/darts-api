variable "resource_group_name" {
  default = "darts-insights-dev-rg"
  description = "Name of the resource group"
}

variable "resource_group_location" {
  default = "uksouth"
  description = "Location of the resource group"
}

variable "resource_group_tags" {  
    default = { 
      environment = "development"
      application = "darts-api"
      BuiltFrom = "https://github.com/hmcts/darts-api"
    }
    description = "Tags proposed for the resource group"
}