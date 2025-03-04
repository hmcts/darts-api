ALTER TABLE rps_retainer
    DROP COLUMN cas_id;

ALTER TABLE rps_retainer
    ADD COLUMN case_object_id CHARACTER VARYING;

ALTER TABLE rps_retainer
    ADD COLUMN audio_folder_object_id CHARACTER VARYING;
