INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (1,'AutomatedTaskOne','Simple test of automated task 1','*/10 * * * * *',true);

INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (2,'AutomatedTaskTwo','Simple test of automated task 2','*/5 * * * * *',true);
