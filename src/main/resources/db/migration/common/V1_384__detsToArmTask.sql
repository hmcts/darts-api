INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size,
created_ts, created_by,last_modified_ts,last_modified_by)
VALUES (nextval('aut_seq'),'DETSToArmDataStore','Move files from DETS to ARM data store','0 9 * * * *',true,100000,
current_timestamp, 0 , current_timestamp,0);