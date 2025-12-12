ALTER TABLE IF EXISTS wk_case_correction ADD COLUMN ret_conf_score INTEGER NULL;
ALTER TABLE IF EXISTS wk_case_correction ADD COLUMN ret_conf_reason CHARACTER VARYING NULL;
ALTER TABLE IF EXISTS wk_case_correction ADD COLUMN merge_action_group INTEGER NULL;
ALTER TABLE IF EXISTS wk_case_correction ADD COLUMN merge_action_subgroup INTEGER NULL;

ALTER TABLE IF EXISTS retention_process_log DROP COLUMN process_ts;
ALTER TABLE IF EXISTS retention_process_log ADD COLUMN process_start_ts TIMESTAMP WITH TIME ZONE NULL;
ALTER TABLE IF EXISTS retention_process_log ADD COLUMN process_end_ts TIMESTAMP WITH TIME ZONE NULL;
ALTER TABLE IF EXISTS retention_process_log ADD COLUMN is_court_case_updated BOOLEAN;
