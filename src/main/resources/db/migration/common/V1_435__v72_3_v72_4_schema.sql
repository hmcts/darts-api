ALTER TABLE annotation_document
    ADD COLUMN data_ticket INTEGER;

ALTER TABLE arm_rpo_execution_detail
    ADD COLUMN polling_created_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE arm_rpo_execution_detail
    ADD COLUMN production_name CHARACTER VARYING;

ALTER TABLE daily_list
    ADD COLUMN data_ticket INTEGER;

ALTER TABLE event
    DROP COLUMN folder_path;

ALTER TABLE external_object_directory
    ADD COLUMN is_dets BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN input_upload_filename CHARACTER VARYING;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN create_record_filename CHARACTER VARYING;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN create_record_processed_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN upload_file_filename CHARACTER VARYING;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN upload_file_processed_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN create_rec_inv_filename CHARACTER VARYING;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN create_rec_inv_processed_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN upload_file_inv_filename CHARACTER VARYING;

ALTER TABLE extobjdir_process_detail
    ADD COLUMN upload_file_inv_processed_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE extobjdir_process_detail
    ADD CONSTRAINT epd_created_by_fk
        FOREIGN KEY (created_by) REFERENCES user_account;

ALTER TABLE extobjdir_process_detail
    ADD CONSTRAINT epd_last_modified_by_fk
        FOREIGN KEY (last_modified_by) REFERENCES user_account;

ALTER TABLE media
    DROP COLUMN folder_path;

ALTER TABLE media
    ADD COLUMN data_ticket INTEGER;

ALTER TABLE object_retrieval_queue
    ADD COLUMN data_ticket INTEGER;

ALTER TABLE transcription
    DROP COLUMN folder_path;

ALTER TABLE transcription
    ADD COLUMN c_current_state CHARACTER VARYING;

ALTER TABLE transcription
    ADD COLUMN r_current_state INTEGER;

ALTER TABLE transcription_document
    ADD COLUMN data_ticket INTEGER;
