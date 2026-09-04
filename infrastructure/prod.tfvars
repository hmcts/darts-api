storage_account_contributor_ids = ["aee94636-8387-4a51-b5a7-a96e580a32d7"]
pgsqlSku                        = "GP_Standard_D16ds_v5"
pgsqlstoragemb                  = "2097152"
db_max_connections              = "5000"

service_criticality = 5

extra_tags = {
  "bcdr-risk-status"    = "v1-unsupported"
  "service_criticality" = "5"
}
