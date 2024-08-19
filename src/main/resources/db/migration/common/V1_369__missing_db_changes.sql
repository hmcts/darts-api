DROP SEQUENCE IF EXISTS audit_activities_seq;
DROP SEQUENCE IF EXISTS audit_seq;
DROP SEQUENCE IF EXISTS cre_seq;

ALTER TABLE annotation_document ALTER COLUMN content_object_id TYPE character varying(16);

ALTER TABLE IF EXISTS audit DROP CONSTRAINT IF EXISTS audit_audit_activity_id_fkey;

DROP INDEX IF EXISTS courthouse_courthouse_name_key;
ALTER TABLE IF EXISTS courthouse ADD CONSTRAINT courthouse_courthouse_name_key UNIQUE (courthouse_name);
DROP INDEX IF EXISTS courthouse_name_unique_idx;

DROP INDEX IF EXISTS courtroom_name_unique_idx;

ALTER TABLE IF EXISTS event ALTER COLUMN is_log_entry DROP DEFAULT;

ALTER TABLE IF EXISTS event_handler ALTER COLUMN is_reporting_restriction DROP DEFAULT;

ALTER TABLE IF EXISTS external_object_directory ALTER COLUMN update_retention DROP DEFAULT;

ALTER TABLE IF EXISTS external_object_directory ALTER COLUMN verification_attempts DROP DEFAULT;

ALTER TABLE IF EXISTS external_object_directory ALTER COLUMN verification_attempts DROP NOT NULL;

ALTER TABLE media ALTER COLUMN content_object_id TYPE character varying(16);
ALTER TABLE IF EXISTS security_group ALTER COLUMN display_state DROP DEFAULT;

ALTER TABLE IF EXISTS security_role ALTER COLUMN display_state DROP DEFAULT;

ALTER TABLE IF EXISTS transcription ALTER COLUMN is_manual_transcription DROP DEFAULT;

ALTER TABLE transcription_document ALTER COLUMN content_object_id TYPE character varying(16);

ALTER TABLE IF EXISTS transcription_urgency ALTER COLUMN display_state DROP DEFAULT;

DROP INDEX user_account_user_email_address_unq;
CREATE UNIQUE INDEX user_account_user_email_address_unq ON user_account (upper(user_email_address)) where is_active;
