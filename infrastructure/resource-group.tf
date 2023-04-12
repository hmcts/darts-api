# resource "azurerm_resource_group" "rg" {
#   count = var.resource_group_name == null ? 1 : 0

#   name     = "${local.name}-data-${var.env}"
#   location = var.location

#   tags = var.common_tags
# }