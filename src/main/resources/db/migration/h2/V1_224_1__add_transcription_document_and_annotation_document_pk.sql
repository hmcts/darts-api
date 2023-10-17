ALTER TABLE transcription_document ADD CONSTRAINT transcription_document_pk PRIMARY KEY (trd_id);
ALTER TABLE annotation_document   ADD CONSTRAINT annotation_document_pk   PRIMARY KEY (ado_id);

ALTER TABLE external_object_directory ADD CONSTRAINT eod_annotation_document_fk FOREIGN KEY (ado_id) REFERENCES annotation_document(ado_id);
ALTER TABLE external_object_directory ADD CONSTRAINT eod_transcription_document_fk FOREIGN KEY (trd_id) REFERENCES transcription_document(trd_id);
