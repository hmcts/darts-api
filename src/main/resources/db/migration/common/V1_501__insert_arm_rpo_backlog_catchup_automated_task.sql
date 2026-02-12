INSERT INTO automated_task
(aut_id, task_name, task_description, cron_expression, cron_editable, batch_size, created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'ArmRpoBacklogCatchup', 'ARM RPO backlog catchup job', '0 21,51 5-18 * * *', true, 1000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account
(usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by,
is_system_user, is_active, user_full_name)
VALUES
(-49, 'system_ArmRpoBacklogCatchup', 'system_ArmRpoBacklogCatchup@hmcts.net', 'system_ArmRpoBacklogCatchup', current_timestamp, current_timestamp, 0, 0,
true, true, 'system_ArmRpoBacklogCatchup');