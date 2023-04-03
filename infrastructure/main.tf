module "postgresql" {

  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  
  source = "git@github.com:hmcts/darts-api?ref=master"
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