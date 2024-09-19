DO $$
DECLARE
	oldId integer;
	sequenceRestartValue integer = 100;
BEGIN
	select nextval('rpt_seq') into oldId;
    if oldId < sequenceRestartValue then
        RAISE NOTICE 'updating rpt_seq from % to %', oldId, sequenceRestartValue;
        perform setval('rpt_seq', sequenceRestartValue, false);
    else
        RAISE NOTICE 'leaving rpt_seq at old value, % ', oldId;
    end if;

END $$;
