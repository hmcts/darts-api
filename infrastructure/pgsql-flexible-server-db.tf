resource "azurerm_postgresql_flexible_server_database" "pg_databases" {
  for_each = {
    for index, db in var.pgsql_databases :
    db.name => db
  }

  name      = each.value.name
  server_id = azurerm_postgresql_flexible_server.pgsql_server.id
  collation = var.collation
  charset   = var.charset
}