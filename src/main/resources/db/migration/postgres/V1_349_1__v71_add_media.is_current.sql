update darts.media set created_ts = current_date where created_ts is null;

UPDATE darts.media m1 set is_current = false where exists (select 1 from darts.media m2 where m1.ctr_id = m2.ctr_id and m1.channel = m2.channel
	and m1.start_ts = m2.start_ts and m1.end_ts = m2.end_ts and m1.media_file = m2.media_file
	and (m2.created_ts >m1.created_ts or (m2.created_ts =m1.created_ts and m2.med_id>m1.med_id)));

UPDATE darts.media set is_current = true where is_current is null;