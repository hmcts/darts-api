--v1	initial version based on obect_state_record.docx 15/3/24
--v2	added parent_id, parent_object_id, content_object_id, object_type, dets_location
--v3    add courthouse_name,cas_id, id_response_cr_file, id_response_uf_file
--v4    amend osr_uuid from character to bigint
--v5    amend tablespace to pg_default
--v6    add revinfo table, as another externally defined object
--v7    add audit_user to revinfo and FK to user_account
--v8    add storage_id and data_ticket to object_state_record
--      add primary key to object_state_record
--      add various indexes to object_state_record
--      remove id_case,courthouse_name,date_last_accessed,flag_file_retained_in_ods from object_state_record
--      remove relation_id,cas_id,parent_id,object_type from object_state_record
--v9    add dal_id to object_state_record
--v10   amend datatypes for eod_id and arm_eod_id to integer


CREATE TABLE object_state_record
(osr_uuid                      BIGINT                     NOT NULL
,eod_id                        INTEGER
,arm_eod_id                    INTEGER
,dal_id                        INTEGER
,parent_object_id              CHARACTER VARYING 
,content_object_id             CHARACTER VARYING   
,id_clip                       CHARACTER VARYING
,dets_location                 CHARACTER VARYING  
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
,object_status                 CHARACTER VARYING
,storage_id                    CHARACTER VARYING
,data_ticket                   INTEGER
) TABLESPACE pg_default;

CREATE UNIQUE INDEX object_state_record_pk              ON object_state_record(osr_uuid) TABLESPACE pg_default; 
ALTER TABLE object_state_record                         ADD PRIMARY KEY USING INDEX object_state_record_pk;

-- multicolumn index, as two columns will be referenced together
CREATE INDEX osr_storage_id_data_ticket                 ON object_state_record(storage_id,data_ticket) TABLESPACE pg_default;

-- only one of the following two indexes should be retained
CREATE INDEX osr_id_clip                                ON object_state_record(id_clip) TABLESPACE pg_default;
-- if the queries that neccesitate this index remain, remove single column, otherwise retain this one,and remove id_clip.
CREATE INDEX osr_id_clip_md5_doc_tx_dets                ON object_state_record(id_clip,md5_doc_transfer_to_dets) TABLESPACE pg_default;

-- obviously if the md5 column is not needed in the 2 column index above, the following would also be redundant
CREATE INDEX osr_md5_doc_tx_dets                        ON object_state_record(md5_doc_transfer_to_dets) TABLESPACE pg_default;

CREATE INDEX osr_content_object_id                      ON object_state_record(content_object_id) TABLESPACE pg_default;
CREATE INDEX osr_flag_file_transfer_to_dets             ON object_state_record(flag_file_transfer_to_dets) TABLESPACE pg_default;


CREATE TABLE revinfo
(rev                           INT4                       NOT NULL
,revtstmp                      INT8 
,audit_user                    INTEGER
,CONSTRAINT revinfo_pkey PRIMARY KEY(rev)
) TABLESPACE pg_default;

ALTER TABLE revinfo 
ADD CONSTRAINT revinfo_audit_user_fk
FOREIGN KEY (audit_user) REFERENCES user_account(usr_id);