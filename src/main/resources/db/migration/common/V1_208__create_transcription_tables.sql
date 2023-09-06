--v46 add transcription_workflow table,
--v47 added table transcription_status


CREATE TABLE darts.transcription_workflow
(trw_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER                       NOT NULL  -- FK to transcription
,workflow_stage              CHARACTER VARYING             NOT NULL  -- will include REQUEST, APPROVAL etc
,workflow_comment            CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

CREATE SEQUENCE trw_seq CACHE 20;


CREATE TABLE darts.transcription_status
(trs_id                      INTEGER                       NOT NULL
,status_type                 CHARACTER VARYING             NOT NULL
);

COMMENT ON TABLE transcription_status
IS 'standing data table';

COMMENT ON COLUMN transcription_status.trs_id
IS 'primary key of transcription_status';

CREATE SEQUENCE trs_seq CACHE 20;










