insert into security_permission (per_id, permission_name)
values (16, 'EXPORT_PROCESSED_PLAYBACK_AUDIO');
insert into security_permission (per_id, permission_name)
values (17, 'EXPORT_PROCESSED_DOWNLOAD_AUDIO');

ALTER SEQUENCE per_seq RESTART WITH 18;


insert into security_role_permission_ae(rol_id, per_id)
values (6, 4);
insert into security_role_permission_ae(rol_id, per_id)
values (6, 6);
insert into security_role_permission_ae(rol_id, per_id)
values (6, 7);
insert into security_role_permission_ae(rol_id, per_id)
values (6, 10);
insert into security_role_permission_ae(rol_id, per_id)
values (6, 13);
insert into security_role_permission_ae(rol_id, per_id)
values (6, 14);


insert into security_role_permission_ae(rol_id, per_id)
values (4, 4);
insert into security_role_permission_ae(rol_id, per_id)
values (5, 4);

insert into security_role_permission_ae(rol_id, per_id)
values (1, 16);
insert into security_role_permission_ae(rol_id, per_id)
values (2, 16);
insert into security_role_permission_ae(rol_id, per_id)
values (3, 16);
insert into security_role_permission_ae(rol_id, per_id)
values (4, 16);
insert into security_role_permission_ae(rol_id, per_id)
values (5, 16);
insert into security_role_permission_ae(rol_id, per_id)
values (6, 16);

insert into security_role_permission_ae(rol_id, per_id)
values (4, 17);

UPDATE security_permission
SET permission_name = 'ACCEPT_TRANSCRIPTION_JOB_REQUEST'
WHERE per_id = 1;

UPDATE security_permission
SET permission_name = 'APPROVE_REJECT_TRANSCRIPTION_REQUEST'
WHERE per_id = 2;

UPDATE security_permission
SET permission_name = 'LISTEN_TO_AUDIO_FOR_DOWNLOAD'
WHERE per_id = 3;

UPDATE security_permission
SET permission_name = 'LISTEN_TO_AUDIO_FOR_PLAYBACK'
WHERE per_id = 4;

UPDATE security_permission
SET permission_name = 'READ_JUDGES_NOTES'
WHERE per_id = 5;

UPDATE security_permission
SET permission_name = 'READ_TRANSCRIBED_DOCUMENT'
WHERE per_id = 6;

UPDATE security_permission
SET permission_name = 'REQUEST_AUDIO'
WHERE per_id = 7;

UPDATE security_permission
SET permission_name = 'REQUEST_TRANSCRIPTION'
WHERE per_id = 8;

UPDATE security_permission
SET permission_name = 'RETENTION_ADMINISTRATION'
WHERE per_id = 9;

UPDATE security_permission
SET permission_name = 'SEARCH_CASES'
WHERE per_id = 10;

UPDATE security_permission
SET permission_name = 'UPLOAD_JUDGES_NOTES'
WHERE per_id = 11;

UPDATE security_permission
SET permission_name = 'UPLOAD_TRANSCRIPTION'
WHERE per_id = 12;

UPDATE security_permission
SET permission_name = 'VIEW_DARTS_INBOX'
WHERE per_id = 13;

UPDATE security_permission
SET permission_name = 'VIEW_MY_AUDIOS'
WHERE per_id = 14;

UPDATE security_permission
SET permission_name = 'VIEW_MY_TRANSCRIPTIONS'
WHERE per_id = 15;

