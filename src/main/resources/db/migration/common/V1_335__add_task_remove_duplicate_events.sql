INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (nextval('aut_seq'),'RemoveDuplicateEvents','Remove duplicate events','0 0 22 * * *', true);
