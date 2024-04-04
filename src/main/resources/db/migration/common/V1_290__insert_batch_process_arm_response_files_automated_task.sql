INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (13,'BatchProcessArmResponseFiles','Batch processes ARM response files','0 5/10 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 14;