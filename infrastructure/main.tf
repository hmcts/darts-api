provider "azurerm" {
  features {}
}
provider "azurerm" {
  features {}
  skip_provider_registration = true
  alias                      = "postgres_network"
  subscription_id            = var.aks_subscription_id
}


locals {
  default_name           = var.component != "" ? "${var.product}-${var.component}" : var.product
  name                   = var.name != "" ? var.name : local.default_name
  server_name            = "${local.name}-${var.env}"
  postgresql_rg_name     = "darts-rg"
  postgresql_rg_location = "UK-South"
  vnet_rg_name           = var.business_area == "sds" ? "ss-${var.env}-network-rg" : "core-infra-${var.env}"
  vnet_name              = var.business_area == "sds" ? "ss-${var.env}-vnet" : "core-infra-vnet-${var.env}"

  private_dns_zone_id = "/subscriptions/1baf5470-1c3e-40d3-a6f7-74bfbce4b348/resourceGroups/core-infra-intsvc-rg/providers/Microsoft.Network/privateDnsZones/private.postgres.database.azure.com"

  is_prod = length(regexall(".*(prod).*", var.env)) > 0

  admin_group    = local.is_prod ? "DTS Platform Operations SC" : "DTS Platform Operations"
  db_reader_user = local.is_prod ? "DTS JIT Access ${var.product} DB Reader SC" : "DTS ${upper(var.business_area)} DB Access Reader"


  high_availability_environments = ["ptl", "perftest", "stg", "aat", "prod"]
  high_availability              = var.high_availability == true || contains(local.high_availability_environments, var.env)



}

data "azurerm_subnet" "pg_subnet" {
  name                 = "postgresql"
  resource_group_name  = local.vnet_rg_name
  virtual_network_name = local.vnet_name

  count = var.pgsql_delegated_subnet_id == "" ? 1 : 0
}

data "azurerm_client_config" "current" {}

data "azuread_group" "db_admin" {
  display_name     = local.admin_group
  security_enabled = true
}

data "azuread_service_principal" "mi_name" {
  count     = var.enable_read_only_group_access ? 1 : 0
  object_id = var.admin_user_object_id
}

module "password" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  length = 20
  # safer set of special characters for pasting in the shell
  override_special = "()-_"
}

module"pgsql_server" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  name                = local.server_name
  resource_group_name = local.postgresql_rg_name
  location            = local.postgresql_rg_location
  version             = var.pgsql_version

  create_mode                       = var.create_mode
  point_in_time_restore_time_in_utc = var.restore_time
  source_server_id                  = var.source_server_id

  delegated_subnet_id = var.pgsql_delegated_subnet_id == "" ? data.azurerm_subnet.pg_subnet[0].id : var.pgsql_delegated_subnet_id
  private_dns_zone_id = local.private_dns_zone_id

  administrator_login    = var.pgsql_admin_username
  administrator_password = random_password.password.result

  storage_mb = var.pgsql_storage_mb

  sku_name = var.pgsql_sku

  authentication {
    active_directory_auth_enabled = true
    tenant_id                     = data.azurerm_client_config.current.tenant_id
    password_auth_enabled         = true
  }

  tags = var.common_tags

  dynamic "high_availability" {
    for_each = local.high_availability != false ? [1] : []
    content {
      mode = "ZoneRedundant"
    }
  }

  maintenance_window {
    day_of_week  = "0"
    start_hour   = "03"
    start_minute = "00"
  }

  lifecycle {
    ignore_changes = [
      zone,
      high_availability.0.standby_availability_zone,
    ]
  }

  backup_retention_days        = var.backup_retention_days
  geo_redundant_backup_enabled = var.geo_redundant_backups

}

module "pgsql_server_config" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  for_each = {
    for index, config in var.pgsql_server_configuration :
    config.name => config
  }

  name      = each.value.name
  server_id = azurerm_postgresql_flexible_server.pgsql_server.id
  value     = each.value.value
}

module"pgsql_adadmin" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  server_name         = azurerm_postgresql_flexible_server.pgsql_server.name
  resource_group_name = azurerm_postgresql_flexible_server.pgsql_server.resource_group_name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  object_id           = data.azuread_group.db_admin.object_id
  principal_name      = local.admin_group
  principal_type      = "Group"
  depends_on = [
    azurerm_postgresql_flexible_server.pgsql_server
  ]
}

module "pgsql_principal_admin" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  count               = var.enable_read_only_group_access ? 1 : 0
  server_name         = azurerm_postgresql_flexible_server.pgsql_server.name
  resource_group_name = azurerm_postgresql_flexible_server.pgsql_server.resource_group_name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  object_id           = var.admin_user_object_id
  principal_name      = data.azuread_service_principal.mi_name[0].display_name
  principal_type      = "ServicePrincipal"
  depends_on = [
    azurerm_postgresql_flexible_server_active_directory_administrator.pgsql_adadmin
  ]
}

module"set-user-permissions-additionaldbs" {
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  for_each = var.enable_read_only_group_access ? { for index, db in var.pgsql_databases : db.name => db } : {}

  triggers = {
    script_hash    = filesha256("${path.module}/set-postgres-permissions.bash")
    name           = local.name
    db_reader_user = local.db_reader_user
  }

  provisioner "local-exec" {
    command = "${path.module}/set-postgres-permissions.bash"

    environment = {
      DB_HOST_NAME   = azurerm_postgresql_flexible_server.pgsql_server.fqdn
      DB_USER        = data.azuread_service_principal.mi_name[0].display_name
      DB_READER_USER = local.db_reader_user
      DB_NAME        = each.value.name
    }
  }
  depends_on = [
    azurerm_postgresql_flexible_server_active_directory_administrator.pgsql_principal_admin,
    azurerm_postgresql_flexible_server_database.pg_databases
  ]
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-shared-${var.env}"
  location = var.location

  tags = var.common_tags
}

module "key-vault" {
  source              = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  name                = "darts-${var.env}"
  product             = var.product
  env                 = var.env
  object_id           = var.jenkins_AAD_objectId
  resource_group_name = azurerm_resource_group.rg.name
  product_group_name  = "DTS Darts Modernisation"
  common_tags         = var.common_tags
  create_managed_identity    = true
}

resource "azurerm_key_vault_secret" "AZURE_APPINSGHTS_KEY" {
  name         = "AppInsightsInstrumentationKey"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.env}"
  location            = var.location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"
  tags                = var.common_tags
}

module "darts-api-db" {

  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env    = var.env

  product       = var.product
  component     = var.component
  business_area = "sds" # sds or cft
  
  pgsql_databases = [
    {
      name : "application"
    }
  ]

  pgsql_version = "14"
  
  # The ID of the principal to be granted admin access to the database server, should be the principal running this normally
  admin_user_object_id = var.admin_user_object_id
  
  common_tags = var.common_tags
}


resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.darts-api-db.username
  key_vault_id = module.key-vault.key_vault_id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.darts-api-db.password
  key_vault_id = module.key-vault.key_vault_id
}

# resource "azurerm_key_vault_secret" "POSTGRES-HOST" {
#   name         = "${var.component}-POSTGRES-HOST"
#   value        = module.darts-api-db.host_name
#   key_vault_id = module.key-vault.key_vault_id
# }

# resource "azurerm_key_vault_secret" "POSTGRES-PORT" {
#   name         = "${var.component}-POSTGRES-PORT"
#   value        = module.darts-api-db.postgresql_listen_port
#   key_vault_id = module.key-vault.key_vault_id
# }

# resource "azurerm_key_vault_secret" "POSTGRES-DATABASE" {
#   name         = "${var.component}-POSTGRES-DATABASE"
#   value        = module.darts-api-db.name
#   key_vault_id = module.key-vault.key_vault_id
# }


