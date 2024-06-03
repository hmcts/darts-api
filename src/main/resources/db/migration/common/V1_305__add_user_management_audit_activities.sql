ALTER SEQUENCE aua_seq RESTART WITH 17;

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Create User', 'Create User', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Deactivate User', 'Deactivate User', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Reactivate User', 'Reactivate User', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Update User', 'Update User', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Create Group', 'Create Group', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Update Group', 'Update Group', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Update Users Group', 'Update User''s Group', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
