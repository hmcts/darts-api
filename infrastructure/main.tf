provider "azurerm" {
  features {}
}

module "db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = var.product
  component          = var.component
  name               = "rpe-${var.product}"
  location           = var.location
  env                = var.env
  database_name      = "darts-modernisation"
  postgresql_user    = "darts-modernisation"
  postgresql_version = "10"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}


module "postgresql" {

  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  
  source = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env    = var.env

  product       = var.product
  component     = var.component
  business_area = "sds" 

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