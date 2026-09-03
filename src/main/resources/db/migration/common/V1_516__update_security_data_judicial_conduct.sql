INSERT INTO security_role (rol_id, role_name, display_name, display_state) VALUES (16, 'JUDICIAL_CONDUCT', 'Judicial Conduct', true);

INSERT INTO security_group (grp_id, rol_id, group_name, global_access, display_state, use_interpreter, display_name, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('grp_seq'), 16, 'JUDICIAL_CONDUCT', true, true, false, 'Judicial Conduct', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);