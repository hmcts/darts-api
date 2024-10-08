DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
	--Prod data will have an id below this value, a deliberate gap will be left between prod data and test data.
	sequenceRestartValue integer = 1000;
BEGIN
perform setval('grp_seq', sequenceRestartValue, false);
RAISE NOTICE 'migrating duplicate records';
FOR recordToBeMigrated IN
    SELECT sg1.grp_id newId, sg2.group_name, sg2.grp_id oldId FROM security_group sg1, security_group sg2
	where upper(sg1.group_name) = upper(sg2.group_name)
    and sg1.grp_id < sg2.grp_id
	 and sg1.grp_id > 0
     and sg2.grp_id >= sequenceRestartValue
     order by 1
	LOOP
		select recordToBeMigrated.oldId into oldId;
    	select recordToBeMigrated.newID into newID;
	RAISE NOTICE 'migrating % from % to %', recordToBeMigrated.group_name, oldId, newId;

	--move entries
	--"security_group_courthouse_ae"
    BEGIN update security_group_courthouse_ae set grp_id = newId where grp_id = oldId; EXCEPTION WHEN OTHERS THEN delete from security_group_courthouse_ae where grp_id = oldId; END;
    	--"security_group_user_account_ae"
    BEGIN update security_group_user_account_ae set grp_id = newId where grp_id = oldId; EXCEPTION WHEN OTHERS THEN delete from security_group_user_account_ae where grp_id = oldId; END;

	--delete old record
	delete from security_group where grp_id = oldId;

    END LOOP;
RAISE NOTICE 'migrating other records';
FOR recordToBeMigrated IN
    SELECT grp_id, group_name from security_group where grp_id >=sequenceRestartValue
    order by 1
	LOOP
	select nextval('grp_seq') into newId;
	select recordToBeMigrated.grp_id into oldId;
	RAISE NOTICE 'migrating % from % to %', recordToBeMigrated.group_name, oldId, newId;

	--insert copy
	INSERT INTO security_group(
		grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
		select newId, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by from security_group where grp_id = oldId;


	--move entries
	--"security_group_courthouse_ae"
    update security_group_courthouse_ae set grp_id = newId where grp_id = oldId;
    --"security_group_user_account_ae"
	update security_group_user_account_ae set grp_id = newId where grp_id = oldId;


	--delete old record
	delete from security_group where grp_id = oldId;

    END LOOP;

END $$;
