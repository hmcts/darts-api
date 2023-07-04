CREATE TABLE IF NOT EXISTS media (
  med_id               INTEGER                     NOT NULL
, crt_id               INTEGER
, r_media_object_id        text
, c_channel                INTEGER
, c_total_channels         INTEGER
, c_reference_id           text
, c_start                  TIMESTAMP without TIME ZONE
, c_end                    TIMESTAMP without TIME ZONE
, c_courtroom              text
, c_reporting_restrictions INTEGER
, c_case_id                text
, r_case_object_id         text
, r_version_label          text
, i_superseded             boolean
, i_version                INTEGER
, CONSTRAINT media_pkey PRIMARY KEY (med_id)
)
