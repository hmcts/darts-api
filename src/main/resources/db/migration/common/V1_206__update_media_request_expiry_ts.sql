ALTER TABLE media_request ADD expiry_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE media_request ADD created_by INTEGER NOT NULL;
ALTER TABLE media_request ADD last_modified_by INTEGER NOT NULL;
ALTER TABLE media_request ALTER COLUMN request_status SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN request_type SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN start_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN end_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN last_modified_ts SET NOT NULL;

ALTER TABLE event_handler ADD created_by INTEGER;
UPDATE event_handler SET created_by = 0;
ALTER TABLE event_handler ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE external_object_directory ADD created_by INTEGER;
UPDATE external_object_directory SET created_by = 0;
ALTER TABLE external_object_directory ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE transient_object_directory ADD created_by INTEGER;
UPDATE transient_object_directory SET created_by = 0;
ALTER TABLE transient_object_directory ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE user_account ADD created_by INTEGER;
