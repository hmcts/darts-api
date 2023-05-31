ALTER TABLE notification ALTER created_date_time TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE notification ALTER last_updated_date_time TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE audio_request ALTER start_time TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE audio_request ALTER end_time TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE audio_request ALTER created_date_time TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE audio_request ALTER last_updated_date_time TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE moj_media ALTER c_start TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE moj_media ALTER c_end TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE moj_media ALTER r_media_object_id TYPE CHARACTER VARYING(16);
ALTER TABLE moj_media ALTER c_reference_id TYPE CHARACTER VARYING;
ALTER TABLE moj_media ALTER c_courtroom TYPE CHARACTER VARYING;
--ALTER TABLE moj_media ALTER c_case_id TYPE CHARACTER VARYING(32)[];
--ALTER TABLE moj_media ALTER r_case_object_id TYPE CHARACTER VARYING(16)[];
ALTER TABLE moj_media ALTER r_version_label TYPE CHARACTER VARYING(32);

CREATE SEQUENCE notification_seq IF NOT EXISTS;
CREATE SEQUENCE audio_request_seq IF NOT EXISTS;
CREATE SEQUENCE moj_med_seq IF NOT EXISTS;
