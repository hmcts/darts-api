INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'CleanUpDetsData', 'Cleans up Dets files that have successfully been stored in ARM', '0 24 0-6,19-23 ? * *', true, 100_000,
        current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by, is_system_user,
                          is_active, user_full_name)

VALUES (-51, 'systemCleanUpDetsDataAutomatedTask', 'systemCleanUpDetsDataAutomatedTask@hmcts.net', 'systemCleanUpDetsDataAutomatedTask',
        '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00', 0, 0, true, true, 'systemCleanUpDetsDataAutomatedTask');