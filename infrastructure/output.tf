
# Resource Group outputs

output "resource_group_id" {
  value = azurerm_resource_group.rg.id
}
output "resource_group_name" {
  value = azurerm_resource_group.rg.name
}

output "resource_group_tags" {
  value = azurerm_resource_group.rg.tags
}

# Application Insights outputs

output "app_insights_id" {
  value = azurerm_application_insights.appinsights.app_id
}

output "app_insights_name" {
  value = azurerm_application_insights.appinsights.name
}

output "app_insights_resource_group_name" {
  value = azurerm_application_insights.appinsights.resource_group_name
}
