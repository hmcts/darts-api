ALTER TABLE object_directory_status
  RENAME TO object_record_status;
ALTER TABLE object_record_status
  RENAME column ods_id to ors_id;
ALTER TABLE object_record_status
  RENAME column ods_description to ors_description;
ALTER TABLE object_record_status
  RENAME CONSTRAINT object_directory_status_pk TO object_record_status_pk;
CREATE SEQUENCE ors_seq;
DROP SEQUENCE IF EXISTS ods_seq;

ALTER TABLE annotation_document
  ADD COLUMN content_object_id CHARACTER VARYING;
ALTER TABLE annotation_document
  ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE court_case
  ADD COLUMN retain_until_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE daily_list
  ADD COLUMN message_id CHARACTER VARYING;


ALTER TABLE external_object_directory
  RENAME column ods_id to ors_id;
ALTER TABLE external_object_directory
  DROP CONSTRAINT eod_object_directory_status_fk; --check if transient object directory needs to do same

ALTER TABLE external_object_directory
  ADD CONSTRAINT eod_object_record_status_fk
    FOREIGN KEY (ors_id) REFERENCES object_record_status (ors_id);

ALTER TABLE media
  ADD COLUMN content_object_id CHARACTER VARYING;
ALTER TABLE media
  ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE media
  ADD COLUMN media_status CHARACTER VARYING;

CREATE TABLE transformed_media
(
  trm_id           INTEGER                  NOT NULL,
  mer_id           INTEGER                  NOT NULL -- FK to media_request
  ,
  last_accessed_ts TIMESTAMP WITH TIME ZONE,
  expiry_ts        TIMESTAMP WITH TIME ZONE,
  output_filename  CHARACTER VARYING,
  output_filesize  INTEGER,
  output_format    CHARACTER VARYING,
  checksum         CHARACTER VARYING,
  start_ts         TIMESTAMP WITH TIME ZONE NOT NULL,
  end_ts           TIMESTAMP WITH TIME ZONE NOT NULL,
  created_ts       TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by       INTEGER                  NOT NULL,
  last_modified_ts TIMESTAMP WITH TIME ZONE NOT NULL,
  last_modified_by INTEGER                  NOT NULL
);

ALTER TABLE transformed_media
  ADD CONSTRAINT trm_media_request_fk
    FOREIGN KEY (mer_id) REFERENCES media_request (mer_id);



