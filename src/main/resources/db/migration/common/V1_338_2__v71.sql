
CREATE TABLE object_state_record
(osr_uuid                      CHARACTER VARYING                     NOT NULL
,eod_id                        CHARACTER VARYING
,arm_eod_id                    CHARACTER VARYING
,parent_id                     CHARACTER VARYING --
,parent_object_id              CHARACTER VARYING --
,content_object_id             CHARACTER VARYING --
,object_type                   CHARACTER VARYING --
,id_clip                       CHARACTER VARYING
,id_case                       CHARACTER VARYING
,courthouse_name               CHARACTER VARYING
,cas_id                        INTEGER
,date_last_accessed            TIMESTAMP WITH TIME ZONE
,relation_id                   CHARACTER VARYING
,dets_location                 CHARACTER VARYING --
,flag_file_transfer_to_dets    BOOLEAN
,date_file_transfer_to_dets    TIMESTAMP WITH TIME ZONE
,md5_doc_transfer_to_dets      CHARACTER VARYING
,file_size_bytes_centera       BIGINT
,file_size_bytes_dets          BIGINT
,flag_file_av_scan_pass        BOOLEAN
,date_file_av_scan_pass        TIMESTAMP WITH TIME ZONE
,flag_file_transf_toarml       BOOLEAN
,date_file_transf_toarml       TIMESTAMP WITH TIME ZONE
,md5_file_transf_arml          CHARACTER VARYING
,file_size_bytes_arml          BIGINT
,flag_file_mfst_created        BOOLEAN
,date_file_mfst_created        TIMESTAMP WITH TIME ZONE
,id_manifest_file              CHARACTER VARYING
,flag_mfst_transf_to_arml      BOOLEAN
,date_mfst_transf_to_arml      TIMESTAMP WITH TIME ZONE
,flag_rspn_recvd_from_arml     BOOLEAN
,date_rspn_recvd_from_arml     TIMESTAMP WITH TIME ZONE
,flag_file_ingest_status       BOOLEAN
,date_file_ingest_to_arm       TIMESTAMP WITH TIME ZONE
,md5_file_ingest_to_arm        CHARACTER VARYING
,file_size_ingest_to_arm       BIGINT
,id_response_file              CHARACTER VARYING
,id_response_cr_file           CHARACTER VARYING
,id_response_uf_file           CHARACTER VARYING
,flag_file_dets_cleanup_status BOOLEAN
,date_file_dets_cleanup        TIMESTAMP WITH TIME ZONE
,flag_file_retained_in_ods     BOOLEAN
,object_status                 CHARACTER VARYING
);