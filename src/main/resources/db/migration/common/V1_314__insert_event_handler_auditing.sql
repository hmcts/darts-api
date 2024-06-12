INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Add Event Mapping', 'Add Event Mapping', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Delete Event Mapping', 'Delete Event Mapping', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Change Event Mapping', 'Change Event Mapping', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);