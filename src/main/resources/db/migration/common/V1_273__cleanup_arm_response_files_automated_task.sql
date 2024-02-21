INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (10,'CleanupArmResponseFiles','Cleans up ARM response files','0 0 21 * * *',true);

ALTER SEQUENCE darts.aut_seq RESTART WITH 11;
