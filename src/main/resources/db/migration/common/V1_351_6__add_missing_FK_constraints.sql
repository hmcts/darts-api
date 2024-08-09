ALTER TABLE retention_policy_type ALTER COLUMN display_name SET NOT NULL;


ALTER TABLE transcription_comment ALTER COLUMN tra_id SET NOT NULL;


update transcription_document theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE transcription_document ADD CONSTRAINT transcription_document_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE transcription_document ALTER COLUMN last_modified_by SET NOT NULL;

ALTER TABLE transcription_type ALTER COLUMN description SET NOT NULL;
ALTER TABLE transcription_urgency ALTER COLUMN description SET NOT NULL;


ALTER TABLE transformed_media ALTER COLUMN start_ts SET NOT NULL;
ALTER TABLE transformed_media ALTER COLUMN end_ts SET NOT NULL;


ALTER TABLE transient_object_directory ALTER COLUMN created_by SET NOT NULL;

update user_account theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE user_account ADD CONSTRAINT user_account_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE user_account ALTER COLUMN created_by SET NOT NULL;

update user_account theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE user_account ADD CONSTRAINT user_account_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE user_account ALTER COLUMN last_modified_by SET NOT NULL;
