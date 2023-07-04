CREATE TABLE IF NOT EXISTS moj_media (
  moj_med_id              INTEGER                     NOT NULL
, moj_ctr_id              INTEGER
, r_media_object_id       CHARACTER VARYING(16)
, c_channel               INTEGER
, c_total_channels        INTEGER
, c_reference_id          CHARACTER VARYING
, c_start                 TIMESTAMP WITH TIME ZONE
, c_end                   TIMESTAMP WITH TIME ZONE
, c_case_id               CHARACTER VARYING(32) ARRAY
, r_case_object_id        CHARACTER VARYING(16) ARRAY
, r_version_label         CHARACTER VARYING(32)
, i_superseded            BOOLEAN
, i_version               INTEGER
, CONSTRAINT moj_media_pk PRIMARY KEY (moj_med_id)
--, CONSTRAINT moj_media_courtroom_fk FOREIGN KEY (moj_ctr_id) REFERENCES moj_courtroom (moj_ctr_id)
);

CREATE SEQUENCE IF NOT EXISTS moj_med_seq;

