INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size,
created_ts, created_by,last_modified_ts,last_modified_by)
VALUES (nextval('aut_seq'),'AudioLinking','Linked cases to appropriate media. ','0 17 0,1,2,3,4,5,6,7,8,20,21,22,23 ? * *',true,7500,
current_timestamp, 0 , current_timestamp,0);