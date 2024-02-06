INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable)
VALUES (9, 'ProcessArmResponseFiles', 'Processes ARM response files', '0 0/10 * * * *', true);

ALTER SEQUENCE aut_seq RESTART WITH 10;

INSERT INTO object_record_status (ors_id, ors_description)
VALUES (16, 'Arm Processing Response Files');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (17, 'Arm Response Process Failed');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (18, 'Arm Response Checksum Verification Failed');

ALTER SEQUENCE ors_seq RESTART WITH 19;

ALTER TABLE external_object_directory
  ADD COLUMN verification_attempts INTEGER NOT NULL DEFAULT 1;

