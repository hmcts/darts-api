ALTER TABLE retention_policy_type ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE transcription_comment ALTER COLUMN tra_id SET NOT NULL;

UPDATE transcription_document transcription_document SET last_modified_by = 0 WHERE last_modified_by IS NULL OR NOT EXISTS (select 1 from user_account ua where ua.usr_id = transcription_document.last_modified_by);

ALTER TABLE transcription_document ADD CONSTRAINT transcription_document_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE transcription_document ALTER COLUMN last_modified_by SET NOT NULL;

ALTER TABLE transcription_type ALTER COLUMN description SET NOT NULL;
ALTER TABLE transcription_urgency ALTER COLUMN description SET NOT NULL;

ALTER TABLE transformed_media ALTER COLUMN start_ts SET NOT NULL;
ALTER TABLE transformed_media ALTER COLUMN end_ts SET NOT NULL;

ALTER TABLE transient_object_directory ALTER COLUMN created_by SET NOT NULL;