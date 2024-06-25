UPDATE security_role
SET display_state = false
WHERE role_name IN ('XHIBIT', 'CPP', 'DAR_PC', 'MID_TIER');

UPDATE security_group
SET display_state = false
WHERE group_name IN ('Xhibit Group', 'Cpp Group', 'Dar Pc Group', 'Mid Tier Group')
