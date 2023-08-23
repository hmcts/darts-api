ALTER TABLE media_request ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE media_request ADD created_by INTEGER NOT NULL AFTER created_ts;
ALTER TABLE media_request ADD last_modified_by INTEGER NOT NULL AFTER last_modified_ts;
ALTER TABLE media_request ADD expiry_ts TIMESTAMP WITH TIME ZONE AFTER last_accessed_ts;

ALTER TABLE event_handler ADD created_by INTEGER AFTER created_ts;
UPDATE event_handler SET created_by = 0;
ALTER TABLE event_handler ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE external_object_directory ADD created_by INTEGER AFTER created_ts;
UPDATE external_object_directory SET created_by = 0;
ALTER TABLE external_object_directory ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE transient_object_directory ADD created_by INTEGER AFTER created_ts;
UPDATE transient_object_directory SET created_by = 0;
ALTER TABLE transient_object_directory ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE user_account ADD created_by INTEGER AFTER created_ts;
