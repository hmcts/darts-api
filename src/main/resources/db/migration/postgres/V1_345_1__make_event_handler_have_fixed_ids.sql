DROP INDEX event_handler_event_type_event_event_sub_type_unq;

DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
BEGIN

FOR recordToBeMigrated IN
    SELECT evh_id from event_handler
    order by 1
	LOOP
	select recordToBeMigrated.evh_id+1000000 into newId;
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

