locals {
  secret_prefix = "${var.component}-POSTGRES"

  secrets = [
    {
      name_suffix = "PASS"
      value       = module.database.postgresql_password
    },
    {
      name_suffix = "HOST"
      value       = module.database.host_name
    },
    {
      name_suffix = "USER"
      value       = module.database.user_name
    },
    {
      name_suffix = "PORT"
      value       = module.database.postgresql_listen_port
    },
    {
      name_suffix = "DATABASE"
      value       = module.database.postgresql_database
    }
  ]

}


## Loop secrets
module "secret" {
  source              = "git@github.com:https://github.com/hmcts/cnp-module-key-vault"
  for_each     = { for secret in local.secrets : secret.name_suffix => secret }
  key_vault_id = data.azurerm_key_vault.kv.id
  name         = "${local.secret_prefix}-${each.value.name_suffix}"
  value        = each.value.value
  tags = merge(var.common_tags, {
    "source" : "${var.component} PostgreSQL"
  })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")

  depends_on = [
    module.database
  ]
}

resource  "sdp-host" {
  source              = "git@github.com:https://github.com/hmcts/cnp-module-key-vault"
  key_vault_id = data.azurerm_key_vault.sdp-kv.id
  name         = "${local.secret_prefix}-HOST"
  value        = module.database.host_name
  tags = merge(var.common_tags, {
    "source" : "${var.component} PostgreSQL"
  })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")

  depends_on = [
    module.database
  ]
}

resource "sdp-port" {
  source              = "git@github.com:https://github.com/hmcts/cnp-module-key-vault"
  key_vault_id = data.azurerm_key_vault.sdp-kv.id
  name         = "${local.secret_prefix}-PORT"
  value        = module.database.postgresql_listen_port
  tags = merge(var.common_tags, {
    "source" : "${var.component} PostgreSQL"
  })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")

  depends_on = [
    module.database
  ]
}

resource "sdp-database" {
  source              = "git@github.com:https://github.com/hmcts/terraform-module-postgresql-flexible"
  key_vault_id = data.azurerm_key_vault.sdp-kv.id
  name         = "${local.secret_prefix}-DATABASE"
  value        = module.database.postgresql_database
  tags = merge(var.common_tags, {
    "source" : "${var.component} PostgreSQL"
  })
  content_type    = ""
  expiration_date = timeadd(timestamp(), "8760h")

  depends_on = [
    module.database
  ]
}

