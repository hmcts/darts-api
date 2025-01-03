CREATE INDEX usr_un_idx ON user_account (user_full_name);

CREATE UNIQUE INDEX extobjdir_process_detail_pk ON extobjdir_process_detail (epd_id);
ALTER TABLE extobjdir_process_detail
    ADD PRIMARY KEY USING INDEX extobjdir_process_detail_pk;

CREATE UNIQUE INDEX transcription_linked_case_pk ON transcription_linked_case (tlc_id);
ALTER TABLE transcription_linked_case
    ADD PRIMARY KEY USING INDEX transcription_linked_case_pk;
