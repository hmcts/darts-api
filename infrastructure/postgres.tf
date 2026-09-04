module "postgresql_flexible" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=DTSPO-30107-additional-postgres-admins"
  env                  = var.env
  product              = var.product
  resource_group_name  = local.rg_name
  component            = var.component
  business_area        = "sds"
  location             = var.location
  pgsql_sku            = var.pgsqlSku
  pgsql_storage_mb     = var.pgsqlstoragemb
  auto_grow_enabled    = true
  common_tags          = merge(var.common_tags, var.extra_tags)
  admin_user_object_id = var.jenkins_AAD_objectId
  enable_qpi           = true
  service_criticality  = var.service_criticality
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]
  pgsql_server_configuration = [
    {
      name  = "azure.extensions"
      value = "pg_stat_statements, pg_trgm, pgaudit"
    },
    {
      name  = "effective_cache_size"
      value = "3211264"
    },
    {
      name  = "effective_io_concurrency"
      value = "200"
    },
    {
      name  = "maintenance_work_mem"
      value = "2097151"
    },
    {
      name  = "max_parallel_workers_per_gather"
      value = "4"
    },
    {
      name  = "max_wal_size"
      value = "4096"
    },
    {
      name  = "min_wal_size"
      value = "1024"
    },
    {
      name  = "random_page_cost"
      value = "1.1"
    },
    {
      name  = "max_connections"
      value = var.db_max_connections
    },
    {
      name  = "pg_qs.retention_period_in_days"
      value = "30"
    },
    {
      name  = "pg_qs.store_query_plans"
      value = "on"
    }
  ]
  pgsql_version = "16"
}

resource "azurerm_key_vault_secret" "POSTGRES-CONNECTION-STRING" {
  name         = "api-POSTGRES-CONNECTION-STRING"
  value        = "postgres://${module.postgresql_flexible.username}:${module.postgresql_flexible.password}@${module.postgresql_flexible.fqdn}:${local.db_port}/${local.db_name}"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "api-POSTGRES-USER"
  value        = module.postgresql_flexible.username
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "api-POSTGRES-PASS"
  value        = module.postgresql_flexible.password
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "api-POSTGRES-HOST"
  value        = module.postgresql_flexible.fqdn
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "api-POSTGRES-PORT"
  value        = local.db_port
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "api-POSTGRES-DATABASE"
  value        = local.db_name
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_postgresql_flexible_server_active_directory_administrator" "jit_admin" {
  server_name         = "darts-api-${var.env}"
  resource_group_name = local.rg_name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  object_id           = data.azuread_group.jit_admin_group.object_id
  principal_name      = data.azuread_group.jit_admin_group.display_name
  principal_type      = "Group"
}
