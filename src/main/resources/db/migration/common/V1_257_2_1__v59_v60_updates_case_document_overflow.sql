CREATE TABLE case_document
(
  cad_id      INTEGER                  NOT NULL,
  cas_id      INTEGER                  NOT NULL,
  file_name   CHARACTER VARYING        NOT NULL,
  file_type   CHARACTER VARYING        NOT NULL,
  file_size   INTEGER                  NOT NULL,
  checksum    CHARACTER VARYING        NOT NULL,
  is_hidden   BOOLEAN                  NOT NULL,
  uploaded_by INTEGER                  NOT NULL,
  uploaded_ts TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE case_overflow
(
  cas_id                    INTEGER NOT NULL,
  case_total_sentence       CHARACTER VARYING,
  retention_event_ts        TIMESTAMP WITH TIME ZONE,
  case_retention_fixed      CHARACTER VARYING,
  retention_applies_from_ts TIMESTAMP WITH TIME ZONE,
  end_of_sentence_date_ts   TIMESTAMP WITH TIME ZONE,
  manual_retention_override INTEGER,
  retain_until_ts           TIMESTAMP WITH TIME ZONE,
  is_standard_policy        BOOLEAN,
  is_permanent_policy       BOOLEAN,
  audio_folder_object_id    CHARACTER VARYING(16)
);


CREATE SEQUENCE cad_seq CACHE 20;

INSERT INTO darts.case_overflow(cas_id, retention_applies_from_ts, end_of_sentence_date_ts, retain_until_ts)
    (select cas_id, retention_applies_from_ts, end_of_sentence_ts, retain_until_ts from darts.court_case);

ALTER TABLE court_case
  DROP COLUMN retention_applies_from_ts;
ALTER TABLE court_case
  DROP COLUMN end_of_sentence_ts;
ALTER TABLE court_case
  DROP COLUMN retain_until_ts;


ALTER TABLE external_object_directory
  ADD COLUMN cad_id INTEGER;-- FK to case_document
ALTER TABLE external_object_directory
  ADD COLUMN manifest_file CHARACTER VARYING;
ALTER TABLE external_object_directory
  ADD COLUMN event_date_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_document
  ADD CONSTRAINT case_document_case_fk
    FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

ALTER TABLE case_overflow
  ADD CONSTRAINT case_overflow_court_case_fk
    FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

