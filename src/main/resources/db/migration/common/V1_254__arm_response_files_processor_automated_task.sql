INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (8,'ProcessArmResponseFiles','Processes ARM response files','0 0/10 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 10;

