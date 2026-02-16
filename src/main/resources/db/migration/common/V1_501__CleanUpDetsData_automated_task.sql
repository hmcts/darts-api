INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                                  created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (35, 'CleanUpDetsData', 'Cleans up Dets files that have successfully been stored in ARM', '0 24 0-6,19-23 ? * *', true, 100_000,
        current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account
VALUES (-40, NULL, '', 'systemCleanUpDetsData@hmcts.net',
        'systemCleanUpDetsDataAutomatedTask', '2025-02-16 00:00:00+00', '2025-02-16 00:00:00+00', NULL, 0, 0, NULL, true,
        true, d 'systemCleanUpDetsDataAutomatedTask');