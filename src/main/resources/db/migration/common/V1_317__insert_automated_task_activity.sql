INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Run Job Manually', 'Run Job Manually', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Enable/Disable Job', 'Enable/Disable Job', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);