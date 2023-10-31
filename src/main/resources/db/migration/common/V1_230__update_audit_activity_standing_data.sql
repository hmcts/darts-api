UPDATE audit_activity
SET created_ts=CURRENT_TIMESTAMP, created_by=0, last_modified_ts=CURRENT_TIMESTAMP, last_modified_by=0
WHERE created_by IS NULL;

ALTER TABLE audit_activity ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE audit_activity ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE audit_activity ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE audit_activity ALTER COLUMN last_modified_by SET NOT NULL;

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by) VALUES (8, 'Download Transcription', 'Download Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by) VALUES (9, 'Authorise Transcription', 'Authorise Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by) VALUES (10, 'Reject Transcription', 'Reject Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by) VALUES (11, 'Accept Transcription', 'Accept Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by) VALUES (12, 'Complete Transcription', 'Complete Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

ALTER SEQUENCE aua_seq RESTART WITH 13;
