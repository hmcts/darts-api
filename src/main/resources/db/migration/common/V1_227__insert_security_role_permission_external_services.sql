insert into security_role (rol_id, role_name)
values (7, 'XHIBIT');
insert into security_role (rol_id, role_name)
values (8, 'CPP');
insert into security_role (rol_id, role_name)
values (9, 'DAR_PC');
insert into security_role (rol_id, role_name)
values (10, 'MID_TIER');

insert into security_permission (per_id, permission_name)
values (18, 'ADD_DOCUMENT');
insert into security_permission (per_id, permission_name)
values (19, 'GET_CASES');
insert into security_permission (per_id, permission_name)
values (20, 'REGISTER_NODE');
insert into security_permission (per_id, permission_name)
values (21, 'ADD_CASE');
insert into security_permission (per_id, permission_name)
values (22, 'ADD_LOG_ENTRY');
insert into security_permission (per_id, permission_name)
values (23, 'ADD_AUDIO');

insert into security_role_permission_ae(rol_id, per_id)
values (7, 16);
insert into security_role_permission_ae(rol_id, per_id)
values (8, 16);
insert into security_role_permission_ae(rol_id, per_id)
values (9, 17);
insert into security_role_permission_ae(rol_id, per_id)
values (9, 18);
insert into security_role_permission_ae(rol_id, per_id)
values (10, 18);
insert into security_role_permission_ae(rol_id, per_id)
values (10, 19);
insert into security_role_permission_ae(rol_id, per_id)
values (10, 20);
insert into security_role_permission_ae(rol_id, per_id)
values (10, 21);

ALTER SEQUENCE rol_seq RESTART WITH 11;
ALTER SEQUENCE per_seq RESTART WITH 24;
