INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by) VALUES (13, 'Import Annotation', 'Import Annotation', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

ALTER SEQUENCE aua_seq RESTART WITH 14;