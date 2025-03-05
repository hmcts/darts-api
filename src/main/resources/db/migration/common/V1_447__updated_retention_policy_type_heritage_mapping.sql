update retention_policy_type_heritage_mapping
set heritage_table = 'dmc_rps_retention_policy'
where heritage_table = 'moj_rps_retention_policy';
