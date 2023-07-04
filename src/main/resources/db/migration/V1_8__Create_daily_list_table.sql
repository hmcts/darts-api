CREATE TABLE IF NOT EXISTS daily_list (
  dal_id                     INTEGER                     NOT NULL
, cth_id                     INTEGER                     NOT NULL
, r_daily_list_object_id         CHARACTER VARYING(16)
, c_unique_id                    CHARACTER VARYING
, c_job_status                   CHARACTER VARYING
, c_timestamp                    TIMESTAMP WITH TIME ZONE
, c_daily_list_id                INTEGER
, c_start_date                   TIMESTAMP WITH TIME ZONE
, c_end_date                     TIMESTAMP WITH TIME ZONE
, c_daily_list_id_s              CHARACTER VARYING
, c_daily_list_source            CHARACTER VARYING
, daily_list_content             CHARACTER VARYING
, r_courthouse_object_id         CHARACTER VARYING
, created_ts                     TIMESTAMP WITH TIME ZONE
, last_modified_ts               TIMESTAMP WITH TIME ZONE
, r_version_label                CHARACTER VARYING(32)
, i_superseded                   BOOLEAN
, i_version                      INTEGER
, CONSTRAINT daily_list_pkey PRIMARY KEY (dal_id)
);

CREATE SEQUENCE IF NOT EXISTS dal_seq;
