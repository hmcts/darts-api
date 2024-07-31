DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
	--Prod data will have an id below this value, a deliberate gap will be left between prod data and test data.
	sequenceRestartValue integer = 1000;
BEGIN
perform setval('aua_seq', sequenceRestartValue, false);
RAISE NOTICE 'migrating duplicate records';
FOR recordToBeMigrated IN
    SELECT table1.aua_id newId, table2.aua_id oldId FROM audit_activity table1, audit_activity table2
	where upper(table1.activity_name) = upper(table2.activity_name)
    and table1.aua_id < table2.aua_id
     and table1.aua_id < sequenceRestartValue
     and table2.aua_id >= sequenceRestartValue
     order by 1
	LOOP
		select recordToBeMigrated.oldId into oldId;
    	select recordToBeMigrated.newID into newID;
	RAISE NOTICE 'migrating from % to %', oldId, newId;

	--move foreign references
	--"audit"
	update audit set aua_id = newId where aua_id = oldId;


	--delete old record
	delete from audit_activity where aua_id = oldId;


    END LOOP;
RAISE NOTICE 'migrating other records';
FOR recordToBeMigrated IN
    SELECT aua_id from audit_activity where aua_id>sequenceRestartValue
    order by 1
	LOOP
	select nextval('aua_seq') into newId;
	select recordToBeMigrated.aua_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	INSERT INTO audit_activity(
    	aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
    	select newId, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by from audit_activity where aua_id = oldId;

	--move foreign references
	--"audit"
	update audit set aua_id = newId where aua_id = oldId;


	--delete old record
	delete from audit_activity where aua_id = oldId;

    END LOOP;
END $$;
