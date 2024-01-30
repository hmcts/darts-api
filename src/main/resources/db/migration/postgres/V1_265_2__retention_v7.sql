CREATE UNIQUE INDEX retention_policy_type_type_unq ON retention_policy_type (fixed_policy_key) where (policy_end_ts is null);
