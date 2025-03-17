ALTER TABLE object_state_record DROP COLUMN loaded_to_osr_ts;

ALTER TABLE case_overflow ALTER COLUMN last_modified_ts DROP DEFAULT;
