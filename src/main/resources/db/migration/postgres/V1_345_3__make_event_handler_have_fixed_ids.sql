DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
	--Prod data will have an id below this value, a deliberate gap will be left between prod data and test data.
	sequenceRestartValue integer = 1000;
BEGIN
perform setval('evh_seq', sequenceRestartValue, false);
RAISE NOTICE 'migrating duplicate records';
FOR recordToBeMigrated IN
    SELECT table1.evh_id newId, table2.evh_id oldId FROM event_handler table1, event_handler table2
	where upper(table1.event_type) = upper(table2.event_type)
    and ((table1.event_sub_type is null and table2.event_sub_type is null) or
    (upper(table1.event_sub_type) = upper(table2.event_sub_type)))
    and table1.evh_id < table2.evh_id
     and table1.evh_id < sequenceRestartValue
     and table2.evh_id >= sequenceRestartValue
     order by 1
	LOOP
		select recordToBeMigrated.oldId into oldId;
    	select recordToBeMigrated.newID into newID;
	RAISE NOTICE 'migrating from % to %', oldId, newId;

	--move foreign references
	--"court_case"
	update court_case set evh_id = newId where evh_id = oldId;
    --"event"
	update event set evh_id = newId where evh_id = oldId;


	--delete old record
	delete from event_handler where evh_id = oldId;


    END LOOP;
RAISE NOTICE 'migrating other records';
FOR recordToBeMigrated IN
    SELECT evh_id from event_handler where evh_id>sequenceRestartValue
    order by 1
	LOOP
	select nextval('evh_seq') into newId;
	select recordToBeMigrated.evh_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	--duplicate record
	INSERT INTO event_handler(
    	evh_id, event_type, event_sub_type, event_name, handler, active, created_ts, created_by, is_reporting_restriction)
    	select newId, event_type, event_sub_type, event_name, handler, active, created_ts, created_by, is_reporting_restriction from event_handler where evh_id = oldId;

	--move foreign references
	--"court_case"
	update court_case set evh_id = newId where evh_id = oldId;
    --"event"
	update event set evh_id = newId where evh_id = oldId;


	--delete old record
	delete from event_handler where evh_id = oldId;

    END LOOP;
END $$;
CREATE UNIQUE INDEX event_handler_event_type_event_event_sub_type_unq ON darts.event_handler (event_type, event_sub_type) where active;
