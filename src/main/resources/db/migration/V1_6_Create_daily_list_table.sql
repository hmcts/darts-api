CREATE TABLE IF NOT EXISTS moj_daily_list (
  moj_dal_id                     INTEGER                     NOT NULL
, moj_crt_id                     INTEGER                     NOT NULL
, r_daily_list_object_id         TEXT                        NOT NULL
, unique_id                      TEXT                        NOT NULL
, status                         TEXT                        NOT NULL
, published_time                 TIMESTAMP WITH TIME ZONE    NOT NULL
, start_date                     TIMESTAMP WITH TIME ZONE    NOT NULL
, end_date                       TIMESTAMP WITH TIME ZONE    NOT NULL
, source                         TEXT                        NOT NULL
, content                        TEXT                        NOT NULL
, created_date_time              TIMESTAMP WITH TIME ZONE    NOT NULL
, last_modified_date             TIMESTAMP WITH time ZONE    NOT NULL
, CONSTRAINT moj_daily_list_pkey PRIMARY KEY (moj_dal_id)
)
