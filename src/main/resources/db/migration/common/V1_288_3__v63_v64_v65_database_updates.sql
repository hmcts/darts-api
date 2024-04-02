CREATE SEQUENCE ohr_seq CACHE 20;

ALTER TABLE annotation_document
ADD CONSTRAINT annotation_document_object_hidden_reason_fk
FOREIGN KEY (ohr_id) REFERENCES object_hidden_reason(ohr_id);

ALTER TABLE case_document
ADD CONSTRAINT case_document_object_hidden_reason_fk
FOREIGN KEY (ohr_id) REFERENCES object_hidden_reason(ohr_id);

ALTER TABLE media
ADD CONSTRAINT media_object_hidden_reason_fk
FOREIGN KEY (ohr_id) REFERENCES object_hidden_reason(ohr_id);

ALTER TABLE transcription_document
ADD CONSTRAINT transcription_document_object_hidden_reason_fk
FOREIGN KEY (ohr_id) REFERENCES object_hidden_reason(ohr_id);

ALTER TABLE case_retention ALTER COLUMN retain_until_applied_on_ts DROP NOT NULL;
