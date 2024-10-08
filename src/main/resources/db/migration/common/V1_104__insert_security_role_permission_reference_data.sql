insert into security_role (rol_id,role_name) values (1,'court manager');
insert into security_role (rol_id,role_name) values (2,'court clerk');
insert into security_role (rol_id,role_name) values (3,'judge');
insert into security_role (rol_id,role_name) values (4,'transcription company');
insert into security_role (rol_id,role_name) values (5,'language shop');

insert into security_permission (per_id,permission_name) values (1,'Accept Transcription Job Request');
insert into security_permission (per_id,permission_name) values (2,'Approve/Reject Transcription Request');
insert into security_permission (per_id,permission_name) values (3,'Listen to Audio for Download');
insert into security_permission (per_id,permission_name) values (4,'Listen to Audio for Playback');
insert into security_permission (per_id,permission_name) values (5,'Read Judges Notes');
insert into security_permission (per_id,permission_name) values (6,'Read Transcribed Document');
insert into security_permission (per_id,permission_name) values (7,'Request Audio');
insert into security_permission (per_id,permission_name) values (8,'Request Transcription');
insert into security_permission (per_id,permission_name) values (9,'Retention administration');
insert into security_permission (per_id,permission_name) values (10,'Search Cases');
insert into security_permission (per_id,permission_name) values (11,'Upload Judges Notes');
insert into security_permission (per_id,permission_name) values (12,'Upload Transcription');
insert into security_permission (per_id,permission_name) values (13,'View DARTS Inbox');
insert into security_permission (per_id,permission_name) values (14,'View My Audios');
insert into security_permission (per_id,permission_name) values (15,'View My Transcriptions');

insert into security_role_permission_ae(rol_id,per_id) values (1,2);
insert into security_role_permission_ae(rol_id,per_id) values (1,4);
insert into security_role_permission_ae(rol_id,per_id) values (1,6);
insert into security_role_permission_ae(rol_id,per_id) values (1,7);
insert into security_role_permission_ae(rol_id,per_id) values (1,8);
insert into security_role_permission_ae(rol_id,per_id) values (1,9);
insert into security_role_permission_ae(rol_id,per_id) values (1,10);
insert into security_role_permission_ae(rol_id,per_id) values (1,13);
insert into security_role_permission_ae(rol_id,per_id) values (1,14);
insert into security_role_permission_ae(rol_id,per_id) values (1,15);
insert into security_role_permission_ae(rol_id,per_id) values (2,4);
insert into security_role_permission_ae(rol_id,per_id) values (2,6);
insert into security_role_permission_ae(rol_id,per_id) values (2,7);
insert into security_role_permission_ae(rol_id,per_id) values (2,8);
insert into security_role_permission_ae(rol_id,per_id) values (2,9);
insert into security_role_permission_ae(rol_id,per_id) values (2,10);
insert into security_role_permission_ae(rol_id,per_id) values (2,13);
insert into security_role_permission_ae(rol_id,per_id) values (2,14);
insert into security_role_permission_ae(rol_id,per_id) values (2,15);
insert into security_role_permission_ae(rol_id,per_id) values (3,4);
insert into security_role_permission_ae(rol_id,per_id) values (3,5);
insert into security_role_permission_ae(rol_id,per_id) values (3,6);
insert into security_role_permission_ae(rol_id,per_id) values (3,7);
insert into security_role_permission_ae(rol_id,per_id) values (3,8);
insert into security_role_permission_ae(rol_id,per_id) values (3,9);
insert into security_role_permission_ae(rol_id,per_id) values (3,10);
insert into security_role_permission_ae(rol_id,per_id) values (3,11);
insert into security_role_permission_ae(rol_id,per_id) values (3,13);
insert into security_role_permission_ae(rol_id,per_id) values (3,14);
insert into security_role_permission_ae(rol_id,per_id) values (3,15);
insert into security_role_permission_ae(rol_id,per_id) values (4,1);
insert into security_role_permission_ae(rol_id,per_id) values (4,3);
insert into security_role_permission_ae(rol_id,per_id) values (4,6);
insert into security_role_permission_ae(rol_id,per_id) values (4,7);
insert into security_role_permission_ae(rol_id,per_id) values (4,10);
insert into security_role_permission_ae(rol_id,per_id) values (4,12);
insert into security_role_permission_ae(rol_id,per_id) values (4,13);
insert into security_role_permission_ae(rol_id,per_id) values (4,14);
insert into security_role_permission_ae(rol_id,per_id) values (4,15);
insert into security_role_permission_ae(rol_id,per_id) values (5,6);
insert into security_role_permission_ae(rol_id,per_id) values (5,7);
insert into security_role_permission_ae(rol_id,per_id) values (5,10);
insert into security_role_permission_ae(rol_id,per_id) values (5,13);
insert into security_role_permission_ae(rol_id,per_id) values (5,14);

ALTER SEQUENCE rol_seq RESTART WITH 6;
ALTER SEQUENCE per_seq RESTART WITH 16;
