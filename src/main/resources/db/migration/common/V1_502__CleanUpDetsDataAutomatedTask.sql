INSERT INTO automated_task
(aut_id, task_name, task_description, cron_expression, cron_editable, batch_size, created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'CleanUpDetsData', 'Clean up DETS data', '0 0 9 30 2 *', true, 100000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account
(usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by,
is_system_user, is_active, user_full_name)
VALUES
(-42, 'system_CleanUpDetsData', 'system_CleanUpDetsData@hmcts.net', 'system_CleanUpDetsData','2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00', 0, 0,
true, true, 'system_CleanUpDetsData');
