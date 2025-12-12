ALTER TABLE IF EXISTS wk_case_correction
ADD COLUMN IF NOT EXISTS ret_conf_score INTEGER;

ALTER TABLE IF EXISTS wk_case_correction
ADD COLUMN IF NOT EXISTS ret_conf_reason CHARACTER VARYING;

ALTER TABLE IF EXISTS wk_case_correction
ADD COLUMN IF NOT EXISTS merge_action_group INTEGER;

ALTER TABLE IF EXISTS wk_case_correction
ADD COLUMN IF NOT EXISTS merge_action_subgroup INTEGER;

ALTER TABLE IF EXISTS retention_process_log
DROP COLUMN IF EXISTS processed_ts;

ALTER TABLE IF EXISTS retention_process_log
ADD COLUMN IF NOT EXISTS process_start_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE IF EXISTS retention_process_log
ADD COLUMN IF NOT EXISTS process_end_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE IF EXISTS retention_process_log
ADD COLUMN IF NOT EXISTS is_court_case_updated BOOLEAN;
