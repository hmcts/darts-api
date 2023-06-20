INSERT INTO moj_courthouse (moj_cth_id, courthouse_name, courthouse_code, created_ts, last_modified_ts) VALUES (1, 'SWANSEA', 457, '2023-05-06 00:00:00+00', '2023-06-14 16:45:09.998033+00');
ALTER SEQUENCE moj_cth_seq RESTART WITH 2;

INSERT INTO audit VALUES (1, 2, 3, 4, '2023-06-13T08:13:09.688537759Z', 'application_server', 'additional_data');
