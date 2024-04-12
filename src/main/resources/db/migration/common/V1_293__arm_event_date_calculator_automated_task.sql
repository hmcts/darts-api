INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (14,'ArmRetentionEventDateCalculator','Sets the retention event date for ARM records','0 0 22 * * *',true);

ALTER SEQUENCE darts.aut_seq RESTART WITH 15;
