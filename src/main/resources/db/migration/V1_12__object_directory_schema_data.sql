CREATE SEQUENCE IF NOT EXISTS ods_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS tod_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS elt_seq CACHE 20;
CREATE SEQUENCE IF NOT EXISTS eod_seq CACHE 20;


CREATE TABLE IF NOT EXISTS object_directory_status (
  ods_id                        INTEGER                     NOT NULL
, ods_description               CHARACTER VARYING
, CONSTRAINT object_directory_status_pk PRIMARY KEY (ods_id)
);

CREATE TABLE IF NOT EXISTS transient_object_directory (
  tod_id                        INTEGER                     NOT NULL
, mer_id                        INTEGER                     NOT NULL
, ods_id                        INTEGER                     NOT NULL
, external_location             UUID                        NOT NULL
, checksum                      CHARACTER VARYING
, transfer_attempts             INTEGER
, created_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_ts                   TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_by                   INTEGER                     --NOT NULL
, CONSTRAINT transient_object_directory_pk PRIMARY KEY (tod_id)
, CONSTRAINT tod_media_request_fk FOREIGN KEY (mer_id) REFERENCES moj_media_request (moj_mer_id) --, CONSTRAINT tod_media_request_fk FOREIGN KEY (mer_id) REFERENCES media_request (mer_id)
--, CONSTRAINT tod_modified_by_fk FOREIGN KEY (modified_by) REFERENCES user_account (usr_id)
, CONSTRAINT tod_object_directory_status_fk FOREIGN KEY (ods_id) REFERENCES object_directory_status (ods_id)
);

CREATE TABLE IF NOT EXISTS external_location_type (
  elt_id                        INTEGER                     NOT NULL
, elt_description               CHARACTER VARYING
, CONSTRAINT external_location_type_pk PRIMARY KEY (elt_id)
);

CREATE TABLE IF NOT EXISTS external_object_directory (
  eod_id                        INTEGER                     NOT NULL
, med_id                        INTEGER
, tra_id                        INTEGER
, ann_id                        INTEGER
, ods_id                        INTEGER                     NOT NULL
, elt_id                        INTEGER                     NOT NULL
, external_location             UUID                        NOT NULL
, checksum                      CHARACTER VARYING
, transfer_attempts             INTEGER
, created_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_ts                   TIMESTAMP WITH TIME ZONE    NOT NULL
, modified_by                   INTEGER                     --NOT NULL
, CONSTRAINT external_object_directory_pk PRIMARY KEY (eod_id)
--, CONSTRAINT eod_annotation_fk FOREIGN KEY (ann_id) REFERENCES annotation (ann_id)
, CONSTRAINT eod_external_location_type_fk FOREIGN KEY (elt_id) REFERENCES external_location_type (elt_id)
, CONSTRAINT eod_media_fk FOREIGN KEY (med_id) REFERENCES moj_media (moj_med_id) --, CONSTRAINT eod_media_fk FOREIGN KEY (med_id) REFERENCES media (med_id)
--, CONSTRAINT eod_modified_by_fk FOREIGN KEY (modified_by) REFERENCES user_account (usr_id)
, CONSTRAINT eod_object_directory_status_fk FOREIGN KEY (ods_id) REFERENCES object_directory_status (ods_id)
--, CONSTRAINT eod_transcription_fk FOREIGN KEY (tra_id) REFERENCES transcription (tra_id)
);


INSERT INTO object_directory_status (ods_id,ods_description) VALUES (1,'New');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (2,'Stored');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (3,'Failure');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (4,'Failure - File not found');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (5,'Failure - File size check failed');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (6,'Failure - File type check failed');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (7,'Failure - Checksum failed');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (8,'Failure - ARM ingestion failed');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (9,'Awaiting Verification');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (10,'Marked for Deletion');
INSERT INTO object_directory_status (ods_id,ods_description) VALUES (11,'Deleted');

ALTER SEQUENCE ods_seq RESTART WITH 12;

INSERT INTO external_location_type (elt_id,elt_description) VALUES (1,'inbound');
INSERT INTO external_location_type (elt_id,elt_description) VALUES (2,'unstructured');
INSERT INTO external_location_type (elt_id,elt_description) VALUES (3,'arm');
INSERT INTO external_location_type (elt_id,elt_description) VALUES (4,'tempstore');
INSERT INTO external_location_type (elt_id,elt_description) VALUES (5,'vodafone');

ALTER SEQUENCE elt_seq RESTART WITH 6;

