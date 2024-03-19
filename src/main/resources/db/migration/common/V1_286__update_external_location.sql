-- replace policy_end_ts with null
UPDATE darts.retention_policy_type SET policy_end_ts = null Where rpt_id = 1