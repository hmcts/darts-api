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



