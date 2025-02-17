ALTER TABLE object_state_record
    ADD COLUMN storage_id CHARACTER VARYING;

ALTER TABLE object_state_record
    ADD COLUMN data_ticket INTEGER;


ALTER TABLE object_state_record
    DROP COLUMN parent_id;

ALTER TABLE object_state_record
    DROP COLUMN object_type;

ALTER TABLE object_state_record
    DROP CONSTRAINT osr_clip_case_unq;
ALTER TABLE object_state_record
    DROP COLUMN id_case;

ALTER TABLE object_state_record
    DROP COLUMN courthouse_name;

ALTER TABLE object_state_record
    DROP COLUMN cas_id;

ALTER TABLE object_state_record
    DROP COLUMN date_last_accessed;

ALTER TABLE object_state_record
    DROP COLUMN relation_id;

ALTER TABLE object_state_record
    DROP COLUMN flag_file_retained_in_ods;

ALTER TABLE object_state_record RENAME CONSTRAINT object_state_record_pkey TO object_state_record_pk;

CREATE INDEX osr_storage_id_data_ticket ON object_state_record (storage_id, data_ticket);
CREATE INDEX osr_id_clip ON object_state_record (id_clip);
CREATE INDEX osr_id_clip_md5_doc_tx_dets ON object_state_record (id_clip, md5_doc_transfer_to_dets);
CREATE INDEX osr_md5_doc_tx_dets ON object_state_record (md5_doc_transfer_to_dets);
CREATE INDEX osr_content_object_id ON object_state_record (content_object_id);
CREATE INDEX osr_flag_file_transfer_to_dets ON object_state_record (flag_file_transfer_to_dets);
