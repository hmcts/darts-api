INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Create Retention Policy', 'Create Retention Policy', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Edit Retention Policy', 'Edit Retention Policy', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Revise Retention Policy', 'Revise Retention Policy', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

