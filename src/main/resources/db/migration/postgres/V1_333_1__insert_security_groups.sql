DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
BEGIN
FOR recordToBeMigrated IN
    SELECT grp_id from security_group where grp_id >0
    order by 1
	LOOP
	select recordToBeMigrated.grp_id+1000000 into newId;
	select recordToBeMigrated.grp_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	--insert copy
	INSERT INTO security_group(
		grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
		select newid, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by from security_group where grp_id = oldId;


	--move entries
	--"security_group_courthouse_ae"
    update security_group_courthouse_ae set grp_id = newId where grp_id = oldId;
    	--"security_group_user_account_ae"
	update security_group_user_account_ae set grp_id = newId where grp_id = oldId;


	--delete old record
	delete from security_group where grp_id = oldId;

    END LOOP;

END $$;
