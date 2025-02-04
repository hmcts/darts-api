update security_group
set dm_group_s_object_id = '1217075880007d00'
where grp_id in (select grp_id from security_group where group_name = 'moj_language_shop');

