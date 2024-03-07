INSERT INTO security_role (rol_id, role_name, display_name, display_state) VALUES (12, 'SUPER_USER', 'Super User', true);

DELETE FROM security_role_permission_ae WHERE rol_id = 11 AND per_id IN (1,2);

INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 3);  -- LISTEN_TO_AUDIO_FOR_DOWNLOAD
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 4);  -- LISTEN_TO_AUDIO_FOR_PLAYBACK
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 6);  -- READ_TRANSCRIBED_DOCUMENT
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 7);  -- REQUEST_AUDIO
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 8);  -- REQUEST_TRANSCRIPTION
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 9);  -- RETENTION_ADMINISTRATION
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 10); -- SEARCH_CASES
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 12); -- UPLOAD_TRANSCRIPTION
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 13); -- VIEW_DARTS_INBOX
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 14); -- VIEW_MY_AUDIOS
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 15); -- VIEW_MY_TRANSCRIPTIONS
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 16); -- EXPORT_PROCESSED_PLAYBACK_AUDIO
INSERT INTO security_role_permission_ae (rol_id, per_id) VALUES (12, 17); -- EXPORT_PROCESSED_DOWNLOAD_AUDIO

INSERT INTO security_group (grp_id, rol_id, group_name, global_access, display_state, use_interpreter, display_name, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('grp_seq'), 12, 'SUPER_USER', true, true, false, 'Super User', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO security_group_user_account_ae (usr_id, grp_id)
SELECT usr.usr_id, grp.grp_id
FROM user_account usr,
security_group grp
WHERE user_name = 'darts_global_test_user'
AND grp.group_name = 'SUPER_USER';

INSERT INTO security_group_user_account_ae (usr_id, grp_id)
SELECT usr.usr_id, grp.grp_id
FROM user_account usr,
security_group grp
WHERE usr.user_name = 'darts_global_test_user'
AND grp.group_name = 'SUPER_ADMIN'
AND NOT EXISTS (
SELECT usr.usr_id, grp.grp_id
FROM security_group_user_account_ae gua
INNER JOIN user_account usr ON gua.usr_id = usr.usr_id
        AND usr.user_name = 'darts_global_test_user'
INNER JOIN security_group grp ON gua.grp_id = grp.grp_id
        AND grp.group_name = 'SUPER_ADMIN'
);
