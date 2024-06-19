delete from audit_activity
where activity_name in (
    'Amend Transcription Workflow',
    'Create Retention Policy',
    'Edit Retention Policy',
    'Revise Retention Policy'
);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (24, 'Amend Transcription Workflow', 'Amend Transcription Workflow', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (25, 'Create Retention Policy', 'Create Retention Policy', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (26, 'Edit Retention Policy', 'Edit Retention Policy', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (27, 'Revise Retention Policy', 'Revise Retention Policy', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

