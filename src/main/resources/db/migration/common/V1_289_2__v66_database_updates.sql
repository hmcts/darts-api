ALTER TABLE annotation
ADD CONSTRAINT annotation_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation_document
ADD CONSTRAINT annotation_document_hidden_by_fk
FOREIGN KEY (hidden_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation_document
ADD CONSTRAINT annotation_document_marked_for_manual_del_by_fk
FOREIGN KEY (marked_for_manual_del_by) REFERENCES user_account(usr_id);

ALTER TABLE case_document
ADD CONSTRAINT case_document_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE case_document
ADD CONSTRAINT case_document_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_document
ADD CONSTRAINT case_document_hidden_by_fk
FOREIGN KEY (hidden_by) REFERENCES user_account(usr_id);

ALTER TABLE case_document
ADD CONSTRAINT case_document_marked_for_manual_del_by_fk
FOREIGN KEY (marked_for_manual_del_by) REFERENCES user_account(usr_id);

ALTER TABLE court_case
ADD CONSTRAINT court_case_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE media
ADD CONSTRAINT media_hidden_by_fk
FOREIGN KEY (hidden_by) REFERENCES user_account(usr_id);

ALTER TABLE media
ADD CONSTRAINT media_marked_for_manual_del_by_fk
FOREIGN KEY (marked_for_manual_del_by) REFERENCES user_account(usr_id);

ALTER TABLE media
ADD CONSTRAINT media_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);


ALTER TABLE transcription_document
ADD CONSTRAINT transcription_document_hidden_by_fk
FOREIGN KEY (hidden_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_document
ADD CONSTRAINT transcription_document_marked_for_manual_del_by_fk
FOREIGN KEY (marked_for_manual_del_by) REFERENCES user_account(usr_id);

ALTER TABLE transformed_media
ADD CONSTRAINT transformed_media_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transformed_media
ADD CONSTRAINT transformed_media_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);