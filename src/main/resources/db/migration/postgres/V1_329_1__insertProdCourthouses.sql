ALTER TABLE darts.courthouse DROP CONSTRAINT courthouse_courthouse_name_key;
DROP INDEX courthouse_name_unique_idx;

DO $$
DECLARE
	chouseToBeMigrated record;
	newId integer;
	oldId integer;
	maxId integer;
BEGIN
    SELECT last_value into maxId FROM cth_seq;
FOR chouseToBeMigrated IN
    SELECT cth_id from darts.courthouse where cth_id between 1 and 151
    union
    SELECT cth_id from darts.courthouse where cth_id > maxId --cleanup data that is above the sequence
    order by 1
	LOOP
	select nextval('cth_seq') into newId;
	select chouseToBeMigrated.cth_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;
	INSERT INTO darts.courthouse(
	cth_id, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name)
	select newId, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name from darts.courthouse where cth_id = oldId;

	--move entries
	--"court_case"
	update darts.court_case set cth_id = newId where cth_id = oldId;
	--"courthouse_region_ae"
	delete from courthouse_region_ae; --repopulating in next script
	--"courtroom"
	update darts.courtroom set cth_id = newId where cth_id = oldId;
	--"security_group_courthouse_ae"
	update darts.security_group_courthouse_ae set cth_id = newId where cth_id = oldId;
	--delete old record
	delete from darts.courthouse where cth_id = oldId;

    END LOOP;
END $$;

