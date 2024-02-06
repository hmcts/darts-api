ALTER TABLE courtroom
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE courtroom
  DROP COLUMN IF EXISTS last_modified_by;

ALTER TABLE event_handler
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE event_handler
  DROP COLUMN IF EXISTS last_modified_by;
ALTER TABLE node_register
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE node_register
  DROP COLUMN IF EXISTS last_modified_by;

ALTER TABLE region
  DROP COLUMN IF EXISTS created_ts;
ALTER TABLE region
  DROP COLUMN IF EXISTS created_by;
ALTER TABLE region
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE region
  DROP COLUMN IF EXISTS last_modified_by;

ALTER TABLE object_directory_status
  DROP COLUMN IF EXISTS created_ts;
ALTER TABLE object_directory_status
  DROP COLUMN IF EXISTS created_by;
ALTER TABLE object_directory_status
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE object_directory_status
  DROP COLUMN IF EXISTS last_modified_by;

ALTER TABLE external_location_type
  DROP COLUMN IF EXISTS created_ts;
ALTER TABLE external_location_type
  DROP COLUMN IF EXISTS created_by;
ALTER TABLE external_location_type
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE external_location_type
  DROP COLUMN IF EXISTS last_modified_by;

ALTER TABLE transcription_status
  DROP COLUMN IF EXISTS created_ts;
ALTER TABLE transcription_status
  DROP COLUMN IF EXISTS created_by;
ALTER TABLE transcription_status
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE transcription_status
  DROP COLUMN IF EXISTS last_modified_by;

ALTER TABLE transcription_type
  DROP COLUMN IF EXISTS created_ts;
ALTER TABLE transcription_type
  DROP COLUMN IF EXISTS created_by;
ALTER TABLE transcription_type
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE transcription_type
  DROP COLUMN IF EXISTS last_modified_by;
ALTER TABLE transcription_workflow
  DROP COLUMN IF EXISTS workflow_comment;

ALTER TABLE transcription_urgency
  DROP COLUMN IF EXISTS created_ts;
ALTER TABLE transcription_urgency
  DROP COLUMN IF EXISTS created_by;
ALTER TABLE transcription_urgency
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE transcription_urgency
  DROP COLUMN IF EXISTS last_modified_by;

ALTER TABLE transcription_workflow
  DROP COLUMN IF EXISTS created_ts;
ALTER TABLE transcription_workflow
  DROP COLUMN IF EXISTS created_by;
ALTER TABLE transcription_workflow
  DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE transcription_workflow
  DROP COLUMN IF EXISTS last_modified_by;
ALTER TABLE transcription_workflow
  DROP COLUMN IF EXISTS workflow_comment;

ALTER TABLE transcription_comment
  ADD COLUMN trw_id INTEGER;

ALTER TABLE transcription_comment
  ADD CONSTRAINT transcription_comment_transcription_workflow_fk
    FOREIGN KEY (trw_id) REFERENCES transcription_workflow (trw_id);

CREATE TABLE IF NOT EXISTS transcription_document
(
  trd_id      INTEGER                  NOT NULL,
  tra_id      INTEGER                  NOT NULL,
  file_name   CHARACTER VARYING        NOT NULL,
  file_type   CHARACTER VARYING        NOT NULL,
  file_size   INTEGER                  NOT NULL,
  uploaded_by INTEGER                  NOT NULL,
  uploaded_ts TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE transcription_document
  ADD CONSTRAINT transcription_document_transcription_fk
    FOREIGN KEY (tra_id) REFERENCES transcription (tra_id);


CREATE TABLE IF NOT EXISTS annotation_document
(
  ado_id      INTEGER                  NOT NULL,
  ann_id      INTEGER                  NOT NULL,
  file_name   CHARACTER VARYING        NOT NULL,
  file_type   CHARACTER VARYING        NOT NULL,
  file_size   INTEGER                  NOT NULL,
  uploaded_by INTEGER                  NOT NULL,
  uploaded_ts TIMESTAMP WITH TIME ZONE NOT NULL
);


ALTER TABLE annotation_document
  ADD CONSTRAINT annotation_document_annotation_fk FOREIGN KEY (ann_id) REFERENCES annotation (ann_id);
ALTER TABLE external_object_directory
  RENAME COLUMN ann_id TO ado_id;
ALTER TABLE annotation
  ADD COLUMN hea_id INTEGER;
ALTER TABLE annotation
  ADD CONSTRAINT annotation_hearing_fk FOREIGN KEY (hea_id) REFERENCES hearing (hea_id);

ALTER TABLE external_object_directory
  RENAME COLUMN tra_id TO trd_id;

ALTER TABLE event_handler
  ADD COLUMN is_reporting_restriction boolean NOT NULL DEFAULT FALSE;


CREATE SEQUENCE IF NOT EXISTS ado_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS trd_seq CACHE 20;

DROP SEQUENCE IF EXISTS node_seq;

ALTER SEQUENCE nod_seq START WITH 50000;





