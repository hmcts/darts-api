UPDATE security_role
SET display_state = false
WHERE role_name = 'XHIBIT'
   OR role_name = 'CPP'
   OR role_name = 'DAR_PC'
   OR role_name = 'MID_TIER';

UPDATE security_group
SET display_state = false
WHERE group_name = 'Xhibit Group'
   OR group_name = 'Cpp Group'
   OR group_name = 'Dar Pc Group'
   OR group_name = 'Mid Tier Group';
