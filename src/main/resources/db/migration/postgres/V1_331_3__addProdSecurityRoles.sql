DO $$
DECLARE
	roleToBeMigrated record;
	newId integer;
	oldId integer;
	maxId integer;
BEGIN
    SELECT last_value into maxId FROM rol_seq;
FOR roleToBeMigrated IN
    SELECT sr1.rol_id newId, sr2.rol_id oldId FROM security_role sr1, security_role sr2
    	where upper(sr1.role_name) = upper(sr2.role_name)
        and sr1.rol_id <> sr2.rol_id
    	 and sr1.rol_id between 1 and 14
    	 order by 1
	LOOP
	select roleToBeMigrated.oldId into oldId;
	select roleToBeMigrated.newID into newID;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	--move entries
	--"security_group"
	update security_group set rol_id = newId where rol_id = oldId;
	--"security_role_permission_ae"
	update security_role_permission_ae set rol_id = newId where rol_id = oldId;

	--delete old record
	delete from security_role where rol_id = oldId;

    END LOOP;
	select max(rol_id) into maxId from security_role;
	perform setval('rol_seq', maxId+1, false);
END $$;
