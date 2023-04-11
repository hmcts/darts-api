variable "resource_group_name" {
  description = "Name of existing resource group to deploy resources into"
  type        = string
  default     = null
}

# variable "location" {
#   description = "Target Azure location to deploy the resource"
#   type        = string
#   default     = "UK South"
# }

variable "pgsql_admin_username" {
  description = "Admin username"
  type        = string
  default     = "pgadmin"
}

variable "pgsql_sku" {
  description = "The PGSql flexible server instance sku"
  type        = string
  default     = "GP_Standard_D2s_v3"
}

variable "pgsql_storage_mb" {
  description = "Max storage allowed for the PGSql Flexibile instance"
  type        = number
  default     = 65536
}

variable "pgsql_server_configuration" {
  description = "Postgres server configuration"
  type        = list(object({ name : string, value : string }))
  default = [{
    name  = "backslash_quote"
    value = "on"
  }]
}

variable "pgsql_firewall_rules" {
  description = "Postgres firewall rules"
  type        = list(object({ name : string, start_ip_address : string, end_ip_address : string }))
  default     = []
}

variable "name" {
  default     = ""
  description = "The default name will be product+component+env, you can override the product+component part by setting this"
}

variable "backup_retention_days" {
  default     = 35
  description = "Backup retention period in days for the PGSql instance. Valid values are between 7 & 35 days"
}

variable "geo_redundant_backups" {
  default     = false
  description = "Enable geo-redundant backups for the PGSql instance."
}

variable "create_mode" {
  default     = "Default"
  description = "The creation mode which can be used to restore or replicate existing servers"
}

variable "restore_time" {
  default     = null
  description = "The point in time to restore. Only used when create mode is set to PointInTimeRestore"
}

variable "source_server_id" {
  default     = null
  description = "Source server ID for point in time restore. Only used when create mode is set to PointInTimeRestore"
}

variable "admin_user_object_id" {
  default     = null
  description = "The ID of the principal to be granted admin access to the database server, should be the principal running this normally"
}

variable "enable_read_only_group_access" {
  type        = bool
  default     = true
  description = "Enables read only group support for accessing the database"
}

variable "collation" {
  type        = string
  default     = "en_GB.utf8"
  description = "Specifies the Collation for the Azure PostgreSQL Flexible Server Database, which needs to be a valid PostgreSQL Collation."
}

variable "charset" {
  type        = string
  default     = "utf8"
  description = "Specifies the Charset for the Azure PostgreSQL Flexible Server Database, which needs to be a valid PostgreSQL Charset."
}

variable "high_availability" {
  type        = bool
  default     = false
  description = "Overrides the automatic selection of high availability mode for the PostgreSQL Flexible Server. Generally you shouldn't set this yourself."
}