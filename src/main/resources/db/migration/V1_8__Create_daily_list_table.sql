CREATE TABLE IF NOT EXISTS moj_daily_list (
  moj_dal_id                     INTEGER                     NOT NULL
, moj_crt_id                     INTEGER                     NOT NULL
, r_daily_list_object_id         TEXT
, c_unique_id                    TEXT
, c_job_status                   TEXT
, c_timestamp                    TIMESTAMP WITH TIME ZONE
, c_daily_list_id                INTEGER
, c_start_date                   TIMESTAMP WITH TIME ZONE
, c_end_date                     TIMESTAMP WITH TIME ZONE
, c_daily_list_id_s              TEXT
, c_daily_list_source            TEXT
, created_ts                     TIMESTAMP WITH TIME ZONE
, last_modified_ts               TIMESTAMP WITH TIME ZONE
, r_version_label                TEXT
, i_superseded                   BOOLEAN
, i_version                      SMALLINT
, CONSTRAINT moj_daily_list_pkey PRIMARY KEY (moj_dal_id)
)

CREATE SEQUENCE IF NOT EXISTS moj_dal_seq;
