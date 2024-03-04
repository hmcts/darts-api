UPDATE security_role
SET role_name = 'SUPER_ADMIN', display_name = 'Super Admin', display_state = true
WHERE rol_id = 11 and role_name = 'ADMIN';

INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 1);  -- ACCEPT_TRANSCRIPTION_JOB_REQUEST
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 2);  -- APPROVE_REJECT_TRANSCRIPTION_REQUEST
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 3);  -- LISTEN_TO_AUDIO_FOR_DOWNLOAD
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 4);  -- LISTEN_TO_AUDIO_FOR_PLAYBACK
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 5);  -- READ_JUDGES_NOTES
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 6);  -- READ_TRANSCRIBED_DOCUMENT
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 7);  -- REQUEST_AUDIO
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 8);  -- REQUEST_TRANSCRIPTION
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 9);  -- RETENTION_ADMINISTRATION
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 10); -- SEARCH_CASES
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 11); -- UPLOAD_JUDGES_NOTES
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 12); -- UPLOAD_TRANSCRIPTION
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 13); -- VIEW_DARTS_INBOX
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 14); -- VIEW_MY_AUDIOS
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 15); -- VIEW_MY_TRANSCRIPTIONS
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 16); -- EXPORT_PROCESSED_PLAYBACK_AUDIO
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (11, 17); -- EXPORT_PROCESSED_DOWNLOAD_AUDIO

UPDATE security_group
SET group_name = 'SUPER_ADMIN', global_access = true, display_state = true, use_interpreter = false, display_name = 'Super Admin', created_ts = CURRENT_TIMESTAMP, created_by = 0, last_modified_ts = CURRENT_TIMESTAMP, last_modified_by = 0
WHERE grp_id = 1 and rol_id = 11 and group_name = 'ADMIN';
