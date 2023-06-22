CREATE TABLE IF NOT EXISTS audit (
  id                    INTEGER                    NOT NULL
, case_id               INTEGER                    NOT NULL
, audit_activity_id     INTEGER                    NOT NULL
, user_id               INTEGER                    NOT NULL
, created_ts            TIMESTAMP with TIME ZONE   NOT NULL
, application_server    CHARACTER VARYING          NOT NULL
, additional_data       CHARACTER VARYING
, CONSTRAINT audit_pkey PRIMARY KEY (id)
);

-- Need to add foreign key for user and case when those tables are created.
CREATE SEQUENCE IF NOT EXISTS audit_seq;
