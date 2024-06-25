INSERT INTO security_role (rol_id, role_name, display_name, display_state)
    VALUES (14, 'MEDIA_ACCESSOR', 'Media Accessor', true);
ALTER SEQUENCE rol_seq RESTART WITH 15;

INSERT INTO security_group (grp_id, rol_id, group_name, global_access, display_state)
    VALUES (nextval('grp_seq'), 14, 'MEDIA_IN_PERPETUITY', true, true);