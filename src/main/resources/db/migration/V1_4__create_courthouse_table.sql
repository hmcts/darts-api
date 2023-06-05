CREATE TABLE moj_courthouse (
  moj_crt_id                INTEGER                         GENERATED ALWAYS AS IDENTITY
, courthouse_name           CHARACTER VARYING               NOT NULL
, code                      SMALLINT
, created_date_time         TIMESTAMP without TIME ZONE     NOT NULL
, last_modified_date_time   TIMESTAMP without TIME ZONE     NOT NULL
, CONSTRAINT moj_courthouse_pkey PRIMARY KEY (moj_crt_id)
);

