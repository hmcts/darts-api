CREATE INDEX usr_un_idx ON darts.user_account (user_full_name);

ALTER TABLE extobjdir_process_detail
    ADD CONSTRAINT extobjdir_process_detail_pk PRIMARY KEY (epd_id);

ALTER TABLE transcription_linked_case
    ADD CONSTRAINT transcription_linked_case_pk PRIMARY KEY (tlc_id);
