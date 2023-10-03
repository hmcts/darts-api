-- standing data required

INSERT INTO user_account (usr_id, user_name, description, created_ts, created_by, last_modified_ts, last_modified_by) VALUES (0, 'SYSTEM', 'System User', current_date, 0, current_date, 0);


INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'New', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Stored', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Failure', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Failure - File not found', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Failure - File size check failed', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Failure - File type check failed', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Failure - Checksum failed', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Failure - ARM ingestion failed', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Awaiting Verification', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'marked for Deletion', current_date, 0, current_date, 0);
INSERT INTO object_directory_status (ods_id,ods_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (nextval('ods_seq'),'Deleted', current_date, 0, current_date, 0);


INSERT INTO darts.audit_activity (aua_id, activity_name, activity_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (1, 'Move Courtroom', 'Move Courtroom', current_date, 0, current_date, 0);
INSERT INTO darts.audit_activity (aua_id, activity_name, activity_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (2, 'Export Audio', 'Export Audio', current_date, 0, current_date, 0);
INSERT INTO darts.audit_activity (aua_id, activity_name, activity_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (3, 'Request Audio', 'Request Audio', current_date, 0, current_date, 0);
INSERT INTO darts.audit_activity (aua_id, activity_name, activity_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (4, 'Audio Playback', 'Audio Playback', current_date, 0, current_date, 0);
INSERT INTO darts.audit_activity (aua_id, activity_name, activity_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (5, 'Apply Retention', 'Apply Retention', current_date, 0, current_date, 0);
INSERT INTO darts.audit_activity (aua_id, activity_name, activity_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (6, 'Request Transcription', 'Request Transcription', current_date, 0, current_date, 0);
INSERT INTO darts.audit_activity (aua_id, activity_name, activity_description,created_ts, created_by, last_modified_ts, last_modified_by) VALUES (7, 'Import Transcription', 'Import Transcription', current_date, 0, current_date, 0);

INSERT INTO darts.transcription_status (tra_id, status_type) VALUES (1, 'Requested');
INSERT INTO darts.transcription_status (tra_id, status_type) VALUES (2, 'Awaiting Authorisation');
INSERT INTO darts.transcription_status (tra_id, status_type) VALUES (3, 'With Transcriber');
INSERT INTO darts.transcription_status (tra_id, status_type) VALUES (4, 'Complete');
INSERT INTO darts.transcription_status (tra_id, status_type) VALUES (5, 'Rejected');
