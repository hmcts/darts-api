INSERT INTO darts.courtroom(ctr_id, cth_id, courtroom_name, created_ts, last_modified_ts) VALUES (1, 1, '1', current_timestamp, current_timestamp);

ALTER SEQUENCE ctr_seq RESTART WITH 2;
