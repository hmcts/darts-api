ALTER TABLE case_overflow ALTER COLUMN cof_id DROP DEFAULT;

ALTER TABLE case_overflow ADD COLUMN c_case_id CHARACTER VARYING(32);
ALTER TABLE case_overflow ADD COLUMN c_courthouse CHARACTER VARYING(64);
ALTER TABLE case_overflow ADD COLUMN c_type CHARACTER VARYING(32);
ALTER TABLE case_overflow ADD COLUMN c_upload_priority INTEGER;
ALTER TABLE case_overflow ADD COLUMN c_reporting_restrictions CHARACTER VARYING(128);
ALTER TABLE case_overflow ADD COLUMN case_object_name CHARACTER VARYING(255);
ALTER TABLE case_overflow ADD COLUMN r_folder_path CHARACTER VARYING(740);
ALTER TABLE case_overflow ADD COLUMN c_interpreter_used INTEGER;
