INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'ResetRetentionAfterDocumentChange',
        'Updates the flags to force apply retention to run again if a media, transcription, annotation or case document is added after retention has been applied',
        '0 35 23 * * *', true, 10_000,
        current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by, is_system_user,
                          is_active, user_full_name)

VALUES (-52, 'systemResetRetentionAfterDocumentChangeAutomatedTask', 'systemResetRetentionAfterDocumentChangeAutomatedTask@hmcts.net',
        'systemResetRetentionAfterDocumentChangeAutomatedTask',
        '2024-01-01 00:00:00+00', '2024-01-01 00:00:00+00', 0, 0, true, true, 'systemResetRetentionAfterDocumentChangeAutomatedTask');