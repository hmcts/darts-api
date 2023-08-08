INSERT INTO darts.transcription_type (trt_id, description) VALUES (1,'Sentencing remarks');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (2,'Summing up (including verdict)');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (3,'Antecedents');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (4,'Argument and submission of ruling');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (5,'Court Log');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (6,'Mitigation');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (7,'Proceedings after verdict');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (8,'Prosecution opening of facts');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (9,'Specified Times');
INSERT INTO darts.transcription_type (trt_id, description) VALUES (999,'Other');

ALTER SEQUENCE trt_seq RESTART WITH 11;
