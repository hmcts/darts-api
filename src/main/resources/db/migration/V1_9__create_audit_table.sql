CREATE TABLE IF NOT EXISTS audit (
  id                    INTEGER                    NOT NULL
, case_id               INTEGER                    NOT NULL
, event_id              INTEGER                    NOT NULL
, moj_usr_id            INTEGER                    NOT NULL
, created_ts            TIMESTAMP with TIME ZONE   NOT NULL
, application_server    CHARACTER VARYING          NOT NULL
, additional_data       CHARACTER VARYING
, CONSTRAINT audit_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS audit_seq;
