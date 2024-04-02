ALTER TABLE annotation ADD COLUMN deleted_by INTEGER;
ALTER TABLE annotation ADD COLUMN deleted_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE annotation_document ADD COLUMN hidden_by INTEGER;
ALTER TABLE annotation_document ADD COLUMN hidden_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE annotation_document ADD COLUMN marked_for_manual_del_by INTEGER;
ALTER TABLE annotation_document ADD COLUMN marked_for_manual_del_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_document ADD COLUMN hidden_by INTEGER;
ALTER TABLE case_document ADD COLUMN hidden_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_document ADD COLUMN marked_for_manual_del_by INTEGER;
ALTER TABLE case_document ADD COLUMN marked_for_manual_del_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_document RENAME COLUMN uploaded_by TO created_by;
ALTER TABLE case_document RENAME COLUMN uploaded_ts TO created_ts;

ALTER TABLE court_case ADD COLUMN deleted_by INTEGER;
ALTER TABLE court_case ADD COLUMN deleted_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE media ADD COLUMN hidden_by INTEGER;
ALTER TABLE media ADD COLUMN hidden_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE media ADD COLUMN marked_for_manual_del_by INTEGER;
ALTER TABLE media ADD COLUMN marked_for_manual_del_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE media ADD COLUMN deleted_by INTEGER;
ALTER TABLE media ADD COLUMN deleted_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE transcription ADD COLUMN deleted_by INTEGER;
ALTER TABLE transcription ADD COLUMN deleted_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE transcription_document ADD COLUMN hidden_by INTEGER;
ALTER TABLE transcription_document ADD COLUMN hidden_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE transcription_document ADD COLUMN marked_for_manual_del_by INTEGER;
ALTER TABLE transcription_document ADD COLUMN marked_for_manual_del_ts TIMESTAMP WITH TIME ZONE;
