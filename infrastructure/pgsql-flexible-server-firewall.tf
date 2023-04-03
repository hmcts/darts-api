resource "azurerm_postgresql_flexible_server_firewall_rule" "pg_firewall_rules" {
  for_each = {
    for index, rule in var.pgsql_firewall_rules :
    rule.name => rule
  }

  name             = each.value.name
  server_id        = azurerm_postgresql_flexible_server.pgsql_server.id
  start_ip_address = each.value.start_ip_address
  end_ip_address   = each.value.end_ip_address
}