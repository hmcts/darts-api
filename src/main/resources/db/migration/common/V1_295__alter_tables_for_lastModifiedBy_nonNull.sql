ALTER TABLE transcription_document ALTER COLUMN last_modified_by SET NOT NULL;
ALTER TABLE case_document ALTER COLUMN last_modified_by SET NOT NULL;
ALTER TABLE annotation_document ALTER COLUMN last_modified_by SET NOT NULL;