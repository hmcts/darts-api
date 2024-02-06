INSERT INTO darts.transcription_urgency(tru_id, description, created_by, last_modified_by, created_ts, last_modified_ts)
VALUES (1, 'Standard', 0, 0, now(), now());
INSERT INTO darts.transcription_urgency(tru_id, description, created_by, last_modified_by, created_ts, last_modified_ts)
VALUES (2, 'Overnight', 0, 0, now(), now());
INSERT INTO darts.transcription_urgency(tru_id, description, created_by, last_modified_by, created_ts, last_modified_ts)
VALUES (3, 'Other', 0, 0, now(), now());

ALTER SEQUENCE tru_seq RESTART WITH 4;
