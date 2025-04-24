INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size,
                                  created_ts, created_by,last_modified_ts,last_modified_by, task_enabled)
VALUES (nextval('darts.aut_seq'),'MediaRequestCleanUp','Clean up stuck media requests','0 0 21 ? * * *',true,4000,
        current_timestamp, 0 , current_timestamp,0, false);

INSERT INTO user_account VALUES (-39, NULL, '', 'system_MediaRequestCleanUpAutomated@hmcts.net',
                                 'system_MediaRequestCleanUpAutomated', '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00', NULL, 0, 0, NULL, true,
                                 true, 'system_MediaRequestCleanUpAutomated');