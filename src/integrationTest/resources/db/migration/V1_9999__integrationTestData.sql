INSERT INTO moj_courthouse (moj_cth_id, courthouse_name, courthouse_code, created_ts, last_modified_ts) VALUES (1, 'SWANSEA', 457, '2023-05-06 00:00:00+00', '2023-06-14 16:45:09.998033+00');
ALTER SEQUENCE moj_cth_seq RESTART WITH 2;

INSERT INTO audit_activities VALUES (998, 'test name', 'test description');
INSERT INTO audit VALUES (999, 2, 998, 4, '2023-06-13T08:13:09.688537759Z', 'application_server', 'additional_data');

INSERT INTO moj_media_request (moj_mer_id, moj_hea_id, requestor, request_status, request_type, start_ts, end_ts, created_ts, last_updated_ts)
VALUES (-1, -2, -3, 'OPEN', 'DOWNLOAD', TIMESTAMP WITH TIME ZONE '2023-06-26 13:00:00+00:00', TIMESTAMP WITH TIME ZONE '2023-06-26 13:45:00+00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO moj_media_request (moj_mer_id, moj_hea_id, requestor, request_status, request_type, start_ts, end_ts, created_ts, last_updated_ts)
VALUES (-2, -2, -3, 'OPEN', 'DOWNLOAD', TIMESTAMP WITH TIME ZONE '2023-06-26 14:00:00+01:00', TIMESTAMP WITH TIME ZONE '2023-06-26 14:45:00+01:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
