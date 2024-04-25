UPDATE transcription_document SET last_modified_by=0 WHERE last_modified_by IS NULL;
UPDATE case_document SET last_modified_by=0 WHERE last_modified_by IS NULL;
UPDATE annotation_document SET last_modified_by=0 WHERE last_modified_by IS NULL;

ALTER TABLE transcription_document ALTER COLUMN last_modified_by SET NOT NULL;
ALTER TABLE case_document ALTER COLUMN last_modified_by SET NOT NULL;
ALTER TABLE annotation_document ALTER COLUMN last_modified_by SET NOT NULL;