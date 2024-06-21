DO
$do$
DECLARE
   case_id   int;
   courthouse_name   text;
	temprow record;
BEGIN
FOR temprow IN
        select med_id, ctr_id, UNNEST(case_number) case_number from darts.media where case_number is not null order by 1,2
    LOOP
		select cc.cas_id into case_id from darts.court_case cc, darts.courtroom cr where cr.cth_id = cc.cth_id and cc.case_number = temprow.case_number and cr.ctr_id = temprow.ctr_id;
		if not found then
			select ch.courthouse_name into courthouse_name from darts.courtroom cr, darts.courthouse ch where cr.cth_id = ch.cth_id and cr.ctr_id= temprow.ctr_id;
        	INSERT INTO darts.media_linked_case (mlc_id,med_id, courthouse_name, case_number) VALUES (nextval('mlc_seq'),temprow.med_id,courthouse_name, temprow.case_number);
		else
			INSERT INTO darts.media_linked_case (mlc_id,med_id, cas_id) VALUES (nextval('mlc_seq'),temprow.med_id,case_id);
		end if;
    END LOOP;
END
$do$;