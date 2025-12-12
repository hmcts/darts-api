-- roll forward

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


-- rollback   

ALTER TABLE IF EXISTS wk_case_correction
DROP COLUMN IF EXISTS ret_conf_score;

ALTER TABLE IF EXISTS wk_case_correction
DROP COLUMN IF EXISTS ret_conf_reason;

ALTER TABLE IF EXISTS wk_case_correction 
DROP COLUMN IF EXISTS merge_action_group;

ALTER TABLE IF EXISTS wk_case_correction
DROP COLUMN IF EXISTS merge_action_subgroup;

ALTER TABLE IF EXISTS retention_process_log
ADD COLUMN IF NOT EXISTS processed_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE IF EXISTS retention_process_log
DROP COLUMN IF EXISTS process_start_ts;

ALTER TABLE IF EXISTS retention_process_log
DROP COLUMN IF EXISTS process_end_ts;

ALTER TABLE IF EXISTS retention_process_log
DROP COLUMN IF EXISTS is_court_case_updated;




