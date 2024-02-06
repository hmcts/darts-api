ALTER TABLE external_object_directory
  DROP CONSTRAINT eod_transcription_fk;
ALTER TABLE external_object_directory
  DROP CONSTRAINT eod_annotation_fk;

ALTER TABLE external_object_directory
  ADD CONSTRAINT external_object_directory_created_by_fk
    FOREIGN KEY (created_by) REFERENCES user_account (usr_id);

UPDATE external_object_directory
SET created_by=0
WHERE created_by IS NULL;

ALTER TABLE external_object_directory
  ALTER COLUMN created_by SET NOT NULL;
