ALTER TABLE object_admin_action
ADD CONSTRAINT oaa_annotation_document_fk
FOREIGN KEY (ado_id) REFERENCES annotation_document(ado_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT oaa_case_document_fk
FOREIGN KEY (cad_id) REFERENCES case_document(cad_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT oaa_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT oaa_transcription_document_fk
FOREIGN KEY (trd_id) REFERENCES transcription_document(trd_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT object_admin_action_ohr_id_fk
FOREIGN KEY (ohr_id) REFERENCES object_hidden_reason(ohr_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT object_admin_action_hidden_by_fk
FOREIGN KEY (hidden_by) REFERENCES user_account(usr_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT object_admin_action_marked_for_manual_del_by_fk
FOREIGN KEY (marked_for_manual_del_by) REFERENCES user_account(usr_id);