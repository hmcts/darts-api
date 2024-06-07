INSERT INTO darts.automated_task(
	aut_id, task_name, task_description, cron_expression, cron_editable, created_ts, created_by, last_modified_ts, last_modified_by, task_enabled, batch_size)
    VALUES (16,'BatchCleanupArmResponseFilesAutomatedTask','Delete ARM response files that have been processed','0 9/10 * * * *',true, current_timestamp, 0, current_timestamp, 0, true, 100);

ALTER SEQUENCE aut_seq RESTART WITH 17;
