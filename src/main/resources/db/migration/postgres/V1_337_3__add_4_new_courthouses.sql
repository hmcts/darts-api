DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
	--Prod data will have an id below this value, a deliberate gap will be left between prod data and test data.
	sequenceRestartValue integer = 1000;
BEGIN
perform setval('cth_seq', sequenceRestartValue, false);
RAISE NOTICE 'migrating duplicate records';
FOR recordToBeMigrated IN
    SELECT cth_id from courthouse where cth_id > sequenceRestartValue
    order by 1
	LOOP
	select nextval('cth_seq') into newId;
	select recordToBeMigrated.cth_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	--duplicate record
	INSERT INTO courthouse(
	cth_id, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name)
	select newId, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name from courthouse where cth_id = oldId;

	--move foreign references
	--"court_case"
	update court_case set cth_id = newId where cth_id = oldId;
	--"courthouse_region_ae"
	delete from courthouse_region_ae; --repopulating in next script
	--"courtroom"
	update courtroom set cth_id = newId where cth_id = oldId;
	--"security_group_courthouse_ae"
	update security_group_courthouse_ae set cth_id = newId where cth_id = oldId;

	--delete old record
	delete from courthouse where cth_id = oldId;

    END LOOP;
END $$;

CREATE UNIQUE INDEX courthouse_name_unique_idx on courthouse (UPPER(courthouse_name));
