INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size)
VALUES (nextval('aut_seq'),'GenerateCaseDocumentForRetentionDate','Generate case document when the retention date is near','0 0 20 * * *',true,50);
