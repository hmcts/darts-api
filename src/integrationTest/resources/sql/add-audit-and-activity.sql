INSERT INTO darts.audit_activity(
	aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
	VALUES (998, 'test name', 'test description', '2023-05-06T00:00:00+00', 0, '2023-05-06T00:00:00+00', 0);
INSERT INTO darts.audit(
	aud_id, cas_id, aua_id, usr_id, created_ts, application_server, additional_data, created_by, last_modified_ts, last_modified_by)
	VALUES (999, 3, 998, -1, '2023-06-13T08:13:09.688537759Z', 'application_server', 'additional_data', 0, '2023-05-06T00:00:00+00', 0);
