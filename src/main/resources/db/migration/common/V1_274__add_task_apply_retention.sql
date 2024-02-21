INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (11,'ApplyRetention','Apply retention after 7 days','0 0 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 12;