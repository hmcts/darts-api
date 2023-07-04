CREATE SEQUENCE IF NOT EXISTS moj_ods_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS eod_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS tod_seq CACHE 20;

CREATE TABLE IF NOT EXISTS moj_object_directory_status (
  moj_ods_id                    INTEGER                     NOT NULL
, ods_description               CHARACTER VARYING
, CONSTRAINT moj_object_directory_status_pk PRIMARY KEY (moj_ods_id)
);

CREATE TABLE IF NOT EXISTS transient_object_directory (
  tod_id                        INTEGER                     NOT NULL
, moj_mer_id                    INTEGER                     NOT NULL
, moj_ods_id                    INTEGER                     NOT NULL
, external_location             UUID                        NOT NULL
, checksum                      CHARACTER VARYING
, created_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_ts                   TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_by                   INTEGER                     --NOT NULL
, CONSTRAINT transient_object_directory_pk PRIMARY KEY (tod_id)
, CONSTRAINT tod_media_request_fk FOREIGN KEY (moj_mer_id) REFERENCES moj_media_request (moj_mer_id)
--,CONSTRAINT tod_modified_by_fk FOREIGN KEY (modified_by) REFERENCES moj_user (moj_usr_id)
, CONSTRAINT tod_object_directory_status_fk FOREIGN KEY (moj_ods_id) REFERENCES moj_object_directory_status (moj_ods_id)
);

CREATE TABLE IF NOT EXISTS external_object_directory (
  eod_id                        INTEGER                     NOT NULL
, moj_med_id                    INTEGER
, moj_tra_id                    INTEGER
, moj_ann_id                    INTEGER
, moj_ods_id                    INTEGER                     NOT NULL
, external_location             UUID                        NOT NULL
, external_location_type        CHARACTER VARYING           NOT NULL
, created_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_ts                   TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_by                   INTEGER                     --NOT NULL
, checksum                      CHARACTER VARYING
, attempts                      INTEGER
, CONSTRAINT external_object_directory_pkey PRIMARY KEY (eod_id)
, CONSTRAINT eod_media_fk FOREIGN KEY (moj_med_id) REFERENCES moj_media (moj_med_id)
, CONSTRAINT eod_object_directory_status_fk FOREIGN KEY (moj_ods_id) REFERENCES moj_object_directory_status (moj_ods_id)
);

INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (1,'New');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (2,'Stored');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (3,'Failure');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (4,'Failure - File not found');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (5,'Failure - File size check failed');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (6,'Failure - File type check failed');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (7,'Failure - Checksum failed');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (8,'Failure - ARM ingestion failed');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (9,'Awaiting Verification');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (10,'Marked for Deletion');
INSERT INTO moj_object_directory_status (moj_ods_id,ods_description) VALUES (11,'Deleted');

ALTER SEQUENCE moj_ods_seq RESTART WITH 12;
