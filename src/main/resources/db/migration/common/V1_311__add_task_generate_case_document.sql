INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size)
VALUES (nextval('aut_seq'),'GenerateCaseDocument','Generate case document on case closure','0 * 0/4 * * *',true,50);
