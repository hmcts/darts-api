ALTER TABLE annotation_document ALTER COLUMN ado_id type bigint;
ALTER TABLE case_document ALTER COLUMN cad_id type bigint;
ALTER TABLE case_management_retention ALTER COLUMN eve_id type bigint;
ALTER TABLE transcription ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_document ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_aud ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_comment ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_comment_aud ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_linked_case ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_workflow ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_workflow_aud ALTER COLUMN tra_id type bigint;
ALTER TABLE case_transcription_ae ALTER COLUMN tra_id type bigint;
ALTER TABLE hearing_transcription_ae ALTER COLUMN tra_id type bigint;
ALTER TABLE transcription_document ALTER COLUMN trd_id type bigint;
ALTER TABLE object_retrieval_queue ALTER COLUMN trd_id type bigint;
ALTER TABLE transient_object_directory ALTER COLUMN tod_id type bigint;

ALTER TABLE object_admin_action
    ALTER COLUMN ado_id TYPE bigint,
    ALTER COLUMN cad_id TYPE bigint,
    ALTER COLUMN trd_id TYPE bigint;
