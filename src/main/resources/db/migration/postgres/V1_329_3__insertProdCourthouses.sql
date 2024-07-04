DO $$
DECLARE
	chouseToBeMigrated record;
BEGIN
FOR chouseToBeMigrated IN
    SELECT ch1.cth_id newId, ch2.cth_id oldId FROM darts.courthouse ch1, darts.courthouse ch2
	where upper(ch1.courthouse_name) = upper(ch2.courthouse_name)
    and ch1.cth_id <> ch2.cth_id
	 and ch1.cth_id between 1 and 151
	 order by 1
	LOOP
	RAISE NOTICE 'processing %', chouseToBeMigrated.oldId;
	--move entries
	--"court_case"
	update darts.court_case set cth_id = chouseToBeMigrated.newId where cth_id = chouseToBeMigrated.oldId;
	--"courthouse_region_ae"
	--not needed
	--"courtroom"
	update darts.courtroom set cth_id = chouseToBeMigrated.newId where cth_id = chouseToBeMigrated.oldId;
	--"security_group_courthouse_ae"
	update darts.security_group_courthouse_ae set cth_id = chouseToBeMigrated.newId where cth_id = chouseToBeMigrated.oldId;
	--delete old record
	delete from darts.courthouse where cth_id = chouseToBeMigrated.oldId;

    END LOOP;
END $$;

CREATE UNIQUE INDEX courthouse_name_unique_idx on courthouse (UPPER(courthouse_name));
