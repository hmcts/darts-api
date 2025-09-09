CREATE INDEX IF NOT EXISTS ard_created_ts_idx ON arm_rpo_execution_detail(created_ts);
CREATE INDEX IF NOT EXISTS mer_request_status_idx ON media_request(request_status);
CREATE INDEX IF NOT EXISTS usr_account_guid_idx ON user_account(account_guid);
