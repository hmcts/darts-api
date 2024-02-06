CREATE UNIQUE INDEX transcription_document_pk ON transcription_document (trd_id);
ALTER TABLE transcription_document
  ADD PRIMARY KEY USING INDEX transcription_document_pk;

CREATE UNIQUE INDEX annotation_document_pk ON annotation_document (ado_id);
ALTER TABLE annotation_document
  ADD PRIMARY KEY USING INDEX annotation_document_pk;

ALTER TABLE external_object_directory
  ADD CONSTRAINT eod_annotation_document_fk FOREIGN KEY (ado_id) REFERENCES annotation_document (ado_id);
ALTER TABLE external_object_directory
  ADD CONSTRAINT eod_transcription_document_fk FOREIGN KEY (trd_id) REFERENCES transcription_document (trd_id);
