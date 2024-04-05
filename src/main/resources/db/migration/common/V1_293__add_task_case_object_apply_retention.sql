INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (14,'ApplyRetentionCaseAssociatedObjects','Apply retention to case associated objects','0 0 20 * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 15;