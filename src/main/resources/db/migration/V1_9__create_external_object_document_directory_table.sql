CREATE TABLE IF NOT EXISTS external_object_directory (
  eod_id                         INTEGER                     NOT NULL
, moj_med_id                     INTEGER
, moj_tra_id                     INTEGER
, moj_ann_id                     INTEGER
, external_location              CHARACTER VARYING           NOT NULL
, external_location_type         CHARACTER VARYING           NOT NULL
, created_ts                     TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_by                    INTEGER                     NOT NULL
, status                         INTEGER                     NOT NULL
, checksum                       CHARACTER VARYING
, attempts                       INTEGER
, CONSTRAINT external_object_directory_pkey PRIMARY KEY (eod_id)
, CONSTRAINT moj_med_id FOREIGN KEY(moj_med_id) REFERENCES moj_media(moj_med_id)
);

CREATE SEQUENCE IF NOT EXISTS eod_seq;
