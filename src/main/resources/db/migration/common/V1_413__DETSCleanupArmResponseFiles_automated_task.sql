INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size,
created_ts, created_by,last_modified_ts,last_modified_by, task_enabled)
VALUES (nextval('aut_seq'),'DETSCleanupArmResponseFiles','Clean up data from ARM response folders Asynchronously. ','0 27 23 * * ?',true,4000,
current_timestamp, 0 , current_timestamp,0, false);