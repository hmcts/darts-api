ALTER TABLE audit_activity DROP CONSTRAINT IF EXISTS audit_audit_activity_id_fkey;

DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
BEGIN

FOR recordToBeMigrated IN
    SELECT aua_id from audit_activity
    order by 1
	LOOP
	select recordToBeMigrated.aua_id+1000000 into newId;
	select recordToBeMigrated.aua_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	--duplicate record
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

