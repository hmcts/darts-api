CREATE SEQUENCE IF NOT EXISTS ods_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS eod_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS tod_seq CACHE 20;

CREATE TABLE IF NOT EXISTS object_directory_status (
  ods_id                    INTEGER                     NOT NULL
, ods_description               CHARACTER VARYING
, CONSTRAINT object_directory_status_pk PRIMARY KEY (ods_id)
);

CREATE TABLE IF NOT EXISTS transient_object_directory (
  tod_id                        INTEGER                     NOT NULL
, mer_id                    INTEGER                     NOT NULL
, ods_id                    INTEGER                     NOT NULL
, external_location             UUID                        NOT NULL
, checksum                      CHARACTER VARYING
, created_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_ts                   TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_by                   INTEGER
, CONSTRAINT transient_object_directory_pk PRIMARY KEY (tod_id)
, CONSTRAINT tod_media_request_fk FOREIGN KEY (mer_id) REFERENCES media_request (mer_id)
--,CONSTRAINT tod_modified_by_fk FOREIGN KEY (modified_by) REFERENCES user (usr_id)
, CONSTRAINT tod_object_directory_status_fk FOREIGN KEY (ods_id) REFERENCES object_directory_status (ods_id)
);

CREATE TABLE IF NOT EXISTS external_object_directory (
  eod_id                        INTEGER                     NOT NULL
, med_id                    INTEGER
, tra_id                    INTEGER
, ann_id                    INTEGER
, ods_id                    INTEGER                     NOT NULL
, external_location             UUID                        NOT NULL
, external_location_type        CHARACTER VARYING           NOT NULL
, created_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_ts                   TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_by                   INTEGER                     NOT NULL
, checksum                      CHARACTER VARYING
, attempts                      INTEGER
, CONSTRAINT external_object_directory_pkey PRIMARY KEY (eod_id)
, CONSTRAINT eod_media_fk FOREIGN KEY (med_id) REFERENCES media (med_id)
, CONSTRAINT eod_object_directory_status_fk FOREIGN KEY (ods_id) REFERENCES object_directory_status (ods_id)
);


