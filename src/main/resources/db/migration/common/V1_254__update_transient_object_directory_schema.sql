ALTER TABLE transient_object_directory
  ADD CONSTRAINT transient_object_directory_created_by_fk
    FOREIGN KEY (created_by) REFERENCES user_account (usr_id);

ALTER TABLE transient_object_directory
  ADD CONSTRAINT tod_transformed_media_fk
    FOREIGN KEY (trm_id) REFERENCES transformed_media (trm_id);

ALTER TABLE transient_object_directory
  ADD CONSTRAINT tod_object_record_status_fk
    FOREIGN KEY (ors_id) REFERENCES object_record_status (ors_id);
