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


alter table user_account alter column created_by drop not null;
alter table user_account alter column last_modified_by drop not null;

UPDATE user_account SET created_by = NULL WHERE usr_id=0;
UPDATE user_account SET last_modified_by = NULL WHERE usr_id=0;

UPDATE user_account user_account SET created_by = 0 WHERE usr_id<>0 AND created_by IS NULL OR NOT EXISTS (SELECT 1 FROM user_account ua WHERE ua.usr_id = user_account.created_by);

ALTER TABLE user_account ADD CONSTRAINT user_account_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

UPDATE user_account user_account SET last_modified_by = 0 WHERE usr_id<>0 AND last_modified_by IS NULL OR NOT EXISTS (SELECT 1 FROM user_account ua WHERE ua.usr_id = user_account.last_modified_by);
ALTER TABLE user_account ADD CONSTRAINT user_account_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE user_account
ADD CONSTRAINT checksystemuser CHECK (usr_id != 0 AND created_by!=null AND last_modified_by!=null OR usr_id=0 AND created_by=null AND last_modified_by=null);