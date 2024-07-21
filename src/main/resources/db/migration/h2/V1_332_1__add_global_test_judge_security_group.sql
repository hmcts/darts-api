INSERT INTO security_group (grp_id, rol_id, group_name, global_access, display_state) VALUES (-7, 3, 'Test Judge Global', true, true);
update security_group set rol_id=1 where grp_id=-7;
