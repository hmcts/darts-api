ALTER TABLE transient_object_directory
DROP CONSTRAINT tod_media_request_fk;

ALTER TABLE transient_object_directory
DROP CONSTRAINT tod_object_directory_status_fk;

ALTER TABLE transient_object_directory RENAME column ods_id to ors_id;

ALTER TABLE transient_object_directory ALTER COLUMN mer_id DROP NOT NULL;
ALTER TABLE transient_object_directory ADD COLUMN trm_id INTEGER;

CREATE SEQUENCE trm_seq START WITH 1;
