INSERT INTO darts.transformed_media(
	   trm_id, mer_id, last_accessed_ts, expiry_ts, output_filename, output_filesize, output_format, checksum, start_ts, end_ts, created_ts, created_by, last_modified_ts, last_modified_by)
	(select nextval('trm_seq'), mr.mer_id, mr.last_accessed_ts, mr.expiry_ts, mr.output_filename, null, mr.output_format, tod.checksum, mr.start_ts, mr.end_ts,  mr.created_ts, mr.created_by, mr.last_modified_ts, mr.last_modified_by from darts.media_request mr, darts.transient_object_directory tod
where tod.mer_id = mr.mer_id);


update darts.transient_object_directory tod
set trm_id = (select tm.trm_id from darts.transformed_media tm where tm.mer_id = tod.mer_id);
