ALTER TABLE external_object_directory
    ADD COLUMN IF NOT EXISTS create_record_processed_ts TIMESTAMP WITH TIME ZONE;

UPDATE external_object_directory
SET create_record_processed_ts = input_upload_processed_ts
WHERE create_record_processed_ts is null
  and input_upload_processed_ts is not null;

CREATE INDEX IF NOT EXISTS eod_cr_proc_ts_idx ON external_object_directory (create_record_processed_ts);
