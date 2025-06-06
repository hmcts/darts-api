import {
  to = module.postgresql_flexible.azurerm_postgresql_flexible_server_database.pg_databases["darts"]
  id = "/subscriptions/3eec5bde-7feb-4566-bfb6-805df6e10b90/resourceGroups/darts-test-rg/providers/Microsoft.DBforPostgreSQL/flexibleServers/darts-api-test/databases/darts"
}

import {
  to = module.postgresql_flexible.azurerm_postgresql_flexible_server_active_directory_administrator.pgsql_adadmin
  id = "/subscriptions/3eec5bde-7feb-4566-bfb6-805df6e10b90/resourceGroups/darts-test-rg/providers/Microsoft.DBforPostgreSQL/flexibleServers/darts-api-test/administrators/e7ea2042-4ced-45dd-8ae3-e051c6551789"
}

import {
  to = module.postgresql_flexible.azurerm_postgresql_flexible_server_active_directory_administrator.pgsql_principal_admin[0]
  id = "/subscriptions/3eec5bde-7feb-4566-bfb6-805df6e10b90/resourceGroups/darts-test-rg/providers/Microsoft.DBforPostgreSQL/flexibleServers/darts-api-test/administrators/7ef3b6ce-3974-41ab-8512-c3ef4bb8ae01"
}
