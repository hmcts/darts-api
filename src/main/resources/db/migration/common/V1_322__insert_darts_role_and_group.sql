INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (13, 'DARTS', 'DARTS', false);
ALTER SEQUENCE rol_seq RESTART WITH 14;

INSERT INTO security_group (grp_id, rol_id, group_name, global_access, display_state, use_interpreter, display_name, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('grp_seq'), 13, 'DARTS', true, false, false, 'DARTS', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
