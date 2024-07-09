DO $$
DECLARE
	chouseToBeMigrated record;
	newId integer;
	oldId integer;
	maxId integer;
BEGIN
FOR chouseToBeMigrated IN
    SELECT ch1.cth_id newId, ch2.cth_id oldId FROM courthouse ch1, courthouse ch2
	where upper(ch1.courthouse_name) = upper(ch2.courthouse_name)
    and ch1.cth_id <> ch2.cth_id
	 and ch1.cth_id between 1 and 151
	 order by 1
	LOOP
		select chouseToBeMigrated.oldId into oldId;
    	select chouseToBeMigrated.newID into newID;
        RAISE NOTICE 'migrating % to %', oldId, newId;

        --move foreign references
        --"court_case"
        update court_case set cth_id = newId where cth_id = oldId;
        --"courthouse_region_ae"
        --not needed
        --"courtroom"
        update courtroom set cth_id = newId where cth_id = oldId;
        --"security_group_courthouse_ae"
        update security_group_courthouse_ae set cth_id = newId where cth_id = oldId;

        --delete old record
        delete from courthouse where cth_id = oldId;

    END LOOP;

	select max(cth_id) into maxId from courthouse;
	perform setval('cth_seq', maxId+1, false);

END $$;

CREATE UNIQUE INDEX courthouse_name_unique_idx on courthouse (UPPER(courthouse_name));
