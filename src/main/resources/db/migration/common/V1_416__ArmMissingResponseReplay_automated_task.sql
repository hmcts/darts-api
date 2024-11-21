INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                                  created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (33, 'ArmMissingResponseReplay', 'Replay objects for Missing ARM response files', '0 0 0 31 2 ?', true, 10000,
        current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account
VALUES (-38, NULL, '', 'systemArmMissingResponseReplayAutomatedTask@hmcts.net',
        'systemArmMissingResponseReplayAutomatedTask', '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00', NULL, 0, 0, NULL, true,
        true, 'systemArmMissingResponseReplayAutomatedTask');