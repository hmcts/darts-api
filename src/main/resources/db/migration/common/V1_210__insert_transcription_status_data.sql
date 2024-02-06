INSERT INTO darts.transcription_status
VALUES (1, 'Requested');
INSERT INTO darts.transcription_status
VALUES (2, 'Awaiting Authorisation');
INSERT INTO darts.transcription_status
VALUES (3, 'With Transcriber');
INSERT INTO darts.transcription_status
VALUES (4, 'Complete');
INSERT INTO darts.transcription_status
VALUES (5, 'Rejected');
INSERT INTO darts.transcription_status
VALUES (6, 'Closed');

ALTER SEQUENCE trs_seq RESTART WITH 7;
