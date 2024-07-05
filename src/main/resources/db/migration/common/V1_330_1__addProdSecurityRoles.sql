--ALTER TABLE courthouse DROP CONSTRAINT courthouse_courthouse_name_key;
--DROP INDEX courthouse_name_unique_idx;

DO $$
DECLARE
	roleToBeMigrated record;
	newId integer;
	oldId integer;
	maxId integer;
BEGIN
    SELECT last_value into maxId FROM rol_seq;
FOR roleToBeMigrated IN
    SELECT rol_id from security_role where rol_id between 1 and 14
    union
    SELECT rol_id from security_role where rol_id > maxId --cleanup data that is above the sequence
    order by 1
	LOOP
	select nextval('rol_seq') into newId;
	select roleToBeMigrated.rol_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	--insert copy
	INSERT INTO security_role(
		rol_id, role_name, display_name, display_state)
		select newId, role_name, display_name, display_state from security_role where rol_id = oldId;


	--move entries
	--"security_group"
	update security_group set rol_id = newId where rol_id = oldId;
	--"security_role_permission_ae"
	update security_role_permission_ae set rol_id = newId where rol_id = oldId;

	--delete old record
	delete from security_role where rol_id = oldId;

    END LOOP;

END $$;
