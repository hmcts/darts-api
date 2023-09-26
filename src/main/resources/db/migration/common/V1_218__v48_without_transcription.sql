-- V48 without transcription changes.
ALTER TABLE court_case ALTER COLUMN case_number SET NOT NULL;
ALTER TABLE court_case ALTER COLUMN case_closed SET NOT NULL;
--ALTER TABLE court_case ALTER COLUMN interpreter_used SET NOT NULL;

ALTER TABLE node_register ALTER COLUMN device_type SET DEFAULT 'DAR';

ALTER TABLE hearing ALTER COLUMN hearing_date SET NOT NULL;
--ALTER TABLE hearing ALTER COLUMN scheduled_start_time SET NOT NULL;
ALTER TABLE hearing ALTER COLUMN hearing_is_actual SET NOT NULL;

ALTER TABLE media ALTER COLUMN channel SET NOT NULL;
ALTER TABLE media ALTER COLUMN total_channels SET NOT NULL;
ALTER TABLE media ALTER COLUMN start_ts SET NOT NULL;
ALTER TABLE media ALTER COLUMN end_ts SET NOT NULL;

DROP SEQUENCE IF EXISTS cra_seq;
