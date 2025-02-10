ALTER TABLE annotation_document
    ALTER COLUMN storage_id TYPE CHARACTER VARYING(16);

ALTER TABLE daily_list
    ALTER COLUMN storage_id TYPE CHARACTER VARYING(16);

ALTER TABLE media
    ALTER COLUMN storage_id TYPE CHARACTER VARYING(16);

ALTER TABLE object_retrieval_queue
    ALTER COLUMN storage_id TYPE CHARACTER VARYING(16);

ALTER TABLE transcription_document
    ALTER COLUMN storage_id TYPE CHARACTER VARYING(16);


CREATE INDEX med_chronicle_id_idx ON media (chronicle_id);
