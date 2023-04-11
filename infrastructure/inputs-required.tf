# variable "env" {
#   description = "Environment value."
#   type        = string
# }

# variable "common_tags" {
#   description = "Common tag to be applied to resources."
#   type        = map(string)
# }

# variable "pgsql_databases" {
#   description = "Databases for the pgsql instance."
#   type        = list(object({ name : string, collation : optional(string), charset : optional(string) }))
# }

# variable "pgsql_delegated_subnet_id" {
#   description = "PGSql delegated subnet id."
#   type        = string
#   default     = ""
# }

# variable "pgsql_version" {
#   description = "The PGSql flexible server instance version."
#   type        = string
# }

# variable "product" {
#   description = "https://hmcts.github.io/glossary/#product"
#   type        = string
# }

# variable "business_area" {
#   description = "business_area name - sds or cft."
# }

# variable "component" {
#   description = "https://hmcts.github.io/glossary/#component"
#   type        = string
# }