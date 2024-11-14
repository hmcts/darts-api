INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size,
created_ts, created_by,last_modified_ts,last_modified_by, task_enabled)
VALUES (32,'DETSCleanupArmResponseFiles','Clean up data from ARM response folders (Dets). ','0 27 23 * * ?',true,4000,
current_timestamp, 0 , current_timestamp,0, false);

INSERT INTO user_account VALUES (-37, NULL, '', 'systemDETSCleanupArmResponseFilesAutomatedTask@hmcts.net',
    'systemDETSCleanupArmResponseFilesAutomatedTask', '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00', NULL, 0, 0, NULL, true,
    true, 'systemDETSCleanupArmResponseFilesAutomatedTask');