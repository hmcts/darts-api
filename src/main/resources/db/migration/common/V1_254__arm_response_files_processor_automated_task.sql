INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (8,'ProcessArmResponseFiles','Processes ARM response files','0 0/10 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 10;

INSERT INTO object_record_status (ors_id, ors_description) VALUES (16, 'Arm Processing Response Files');
ALTER SEQUENCE ors_seq RESTART WITH 17;

