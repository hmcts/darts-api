ALTER TABLE court_case
  DROP COLUMN retain_until_ts;
ALTER TABLE court_case
  ADD COLUMN retention_applies_from_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE court_case
  ADD COLUMN end_of_sentence_ts TIMESTAMP WITH TIME ZONE;

DROP TABLE case_retention cascade;
DROP TABLE retention_policy;
DROP TABLE case_retention_event;

DROP SEQUENCE car_seq;

ALTER TABLE annotation
  ADD COLUMN current_owner INTEGER NOT NULL;
ALTER TABLE annotation
  ADD CONSTRAINT annotation_current_owner_fk
    FOREIGN KEY (current_owner) REFERENCES user_account (usr_id);

ALTER TABLE media
  ALTER COLUMN file_size type BIGINT;

ALTER TABLE media
  DROP COLUMN met_id;
DROP TABLE media_type;

ALTER TABLE media
  ADD COLUMN media_type CHAR(1) NOT NULL DEFAULT 'A';

--Retention
CREATE TABLE case_management_retention
(
  cmr_id         INTEGER           NOT NULL,
  cas_id         INTEGER           NOT NULL,
  rpt_id         INTEGER           NOT NULL,
  eve_id         INTEGER           NOT NULL,
  total_sentence CHARACTER VARYING NOT NULL,
  CONSTRAINT case_management_retention_pk PRIMARY KEY (cmr_id)
);

CREATE TABLE case_retention
(
  car_id                     INTEGER                  NOT NULL,
  cas_id                     INTEGER                  NOT NULL,
  rpt_id                     INTEGER                  NOT NULL,
  cmr_id                     INTEGER,
  total_sentence             CHARACTER VARYING        NOT NULL,
  retain_until_ts            TIMESTAMP WITH TIME ZONE NOT NULL,
  retain_until_applied_on_ts TIMESTAMP WITH TIME ZONE NOT NULL,
  current_state              CHARACTER VARYING        NOT NULL,
  comments                   CHARACTER VARYING,
  retention_object_id        CHARACTER VARYING,
  submitted_ts               TIMESTAMP WITH TIME ZONE NOT NULL,
  created_ts                 TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by                 INTEGER                  NOT NULL,
  CONSTRAINT case_retention_pk PRIMARY KEY (car_id)
);

CREATE TABLE retention_policy_type
(
  rpt_id                     INTEGER                  NOT NULL,
  fixed_policy_key           INTEGER                  NOT NULL,
  policy_name                CHARACTER VARYING        NOT NULL,
  duration                   INTEGER                  NOT NULL,
  policy_start_ts            TIMESTAMP WITH TIME ZONE NOT NULL,
  policy_end_ts              TIMESTAMP WITH TIME ZONE NOT NULL,
  retention_policy_object_id CHARACTER VARYING,
  created_ts                 TIMESTAMP WITH TIME ZONE NOT NULL,
  created_by                 INTEGER                  NOT NULL,
  last_modified_ts           TIMESTAMP WITH TIME ZONE NOT NULL,
  last_modified_by           INTEGER                  NOT NULL,
  CONSTRAINT retention_policy_type_pk PRIMARY KEY (rpt_id)
);

CREATE SEQUENCE cmr_seq CACHE 20;
CREATE SEQUENCE car_seq CACHE 20;
CREATE SEQUENCE rpt_seq CACHE 20;

ALTER TABLE case_retention
  ADD CONSTRAINT case_retention_court_case_fk
    FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

ALTER TABLE case_retention
  ADD CONSTRAINT case_retention_retention_policy_type_fk
    FOREIGN KEY (rpt_id) REFERENCES retention_policy_type (rpt_id);

ALTER TABLE case_management_retention
  ADD CONSTRAINT case_management_retention_court_case_fk
    FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

ALTER TABLE case_management_retention
  ADD CONSTRAINT case_management_retention_retention_policy_type_fk
    FOREIGN KEY (rpt_id) REFERENCES retention_policy_type (rpt_id);

ALTER TABLE case_management_retention
  ADD CONSTRAINT case_management_retention_event_fk
    FOREIGN KEY (eve_id) REFERENCES event (eve_id);

ALTER TABLE retention_policy_type
  ADD CONSTRAINT retention_policy_type_created_by_fk
    FOREIGN KEY (created_by) REFERENCES user_account (usr_id);

ALTER TABLE retention_policy_type
  ADD CONSTRAINT retention_policy_type_last_modified_by_fk
    FOREIGN KEY (last_modified_by) REFERENCES user_account (usr_id);

ALTER TABLE case_retention
  ADD CONSTRAINT case_retention_case_management_retention_fk
    FOREIGN KEY (cmr_id) REFERENCES case_management_retention (cmr_id);

-- Security
ALTER TABLE security_group
  ADD COLUMN use_interpreter BOOLEAN NOT null default FALSE;

-- v56
ALTER TABLE transcription
  ADD COLUMN hide_request_from_requestor BOOLEAN;
UPDATE transcription
set hide_request_from_requestor= false;
ALTER TABLE transcription
  ALTER COLUMN hide_request_from_requestor SET NOT NULL;
