INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (11, 'ADMIN', 'Admin', true);
ALTER SEQUENCE rol_seq RESTART WITH 12;

DELETE
FROM security_group
WHERE grp_id > 0;
INSERT INTO security_group (grp_id, rol_id, group_name, group_display_name, global_access, display_state)
VALUES (nextval('grp_seq'), 11, 'ADMIN', 'Admin', true, true);
