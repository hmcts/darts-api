resource "azurerm_resource_group" "rg" {
  location = var.resource_group_location
  name = var.resource_group_name
  tags = var.resource_group_tags
}