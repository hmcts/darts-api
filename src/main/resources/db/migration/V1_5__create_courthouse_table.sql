CREATE TABLE moj_courthouse (
  moj_crt_id                INTEGER                         NOT NULL
, courthouse_name           CHARACTER VARYING               NOT NULL
, courthouse_code           SMALLINT                        UNIQUE
, created_ts         TIMESTAMP with TIME ZONE     NOT NULL
, last_modified_ts   TIMESTAMP with TIME ZONE     NOT NULL
, CONSTRAINT moj_courthouse_pkey PRIMARY KEY (moj_crt_id)
);

CREATE SEQUENCE IF NOT EXISTS moj_crt_seq;


