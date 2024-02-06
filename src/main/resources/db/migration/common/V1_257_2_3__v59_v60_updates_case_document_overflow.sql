ALTER TABLE external_object_directory
  ADD CONSTRAINT eod_case_document_fk
    FOREIGN KEY (cad_id) REFERENCES case_document (cad_id);

