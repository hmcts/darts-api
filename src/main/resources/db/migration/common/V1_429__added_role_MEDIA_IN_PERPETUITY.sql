insert into security_role (rol_id, role_name, display_name, display_state)
values (nextval('rol_seq'), 'MEDIA_IN_PERPETUITY', 'Media in Perpetuity', TRUE);


update security_group
set rol_id = (select rol_id from security_role where role_name = 'MEDIA_IN_PERPETUITY')
where grp_id = 7;

update security_role
set display_state = FALSE
where role_name = 'MEDIA_ACCESSOR';





