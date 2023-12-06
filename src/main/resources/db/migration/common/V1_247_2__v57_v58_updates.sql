ALTER TABLE transient_object_directory
DROP CONSTRAINT tod_media_request_fk;

ALTER TABLE transient_object_directory
DROP CONSTRAINT tod_object_directory_status_fk;

ALTER TABLE transient_object_directory RENAME column ods_id to ors_id;

ALTER TABLE transient_object_directory ALTER COLUMN mer_id DROP NOT NULL;
ALTER TABLE transient_object_directory ADD COLUMN trm_id INTEGER;

--copy values to transformed media and transient object directory
truncate table transformed_media;
truncate table transient_object_directory;

ALTER TABLE media_request DROP COLUMN last_accessed_ts;
ALTER TABLE media_request DROP COLUMN expiry_ts;
ALTER TABLE media_request DROP COLUMN output_filename;
ALTER TABLE media_request DROP COLUMN output_format;

ALTER TABLE transient_object_directory DROP COLUMN mer_id;
ALTER TABLE transient_object_directory ALTER COLUMN trm_id SET NOT NULL;

ALTER TABLE transcription_document ADD COLUMN content_object_id CHARACTER VARYING;
ALTER TABLE transcription_document ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE transcription_urgency ADD COLUMN priority_order INTEGER;
update transcription_urgency set priority_order=1 where tru_id=2;
update transcription_urgency set priority_order=2 where tru_id=7;
update transcription_urgency set priority_order=3 where tru_id=4;
update transcription_urgency set priority_order=4 where tru_id=5;
update transcription_urgency set priority_order=5 where tru_id=6;
update transcription_urgency set priority_order=6 where tru_id=3;
update transcription_urgency set priority_order=999 where tru_id=1;
ALTER TABLE transcription_urgency ALTER COLUMN priority_order SET NOT NULL;



ALTER TABLE user_account ADD COLUMN is_active BOOLEAN;
update user_account set is_active = true;
ALTER TABLE user_account ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE user_account DROP COLUMN user_state;


CREATE SEQUENCE trm_seq START WITH 1;


