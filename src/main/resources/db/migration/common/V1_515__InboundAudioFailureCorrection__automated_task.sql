INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'InboundAudioFailureCorrection', 'Fixes issues where audio where audio ingestion failed', '0 12 4 * * *', true,
        10_000,
        current_timestamp, 0, current_timestamp, 0, true);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by, is_system_user,
                          is_active, user_full_name)

VALUES (-53, 'system_InboundAudioFailureCorrectionAutomatedTask', 'system_InboundAudioFailureCorrectionAutomatedTask@hmcts.net',
        'system_InboundAudioFailureCorrectionAutomatedTask',
        '2026-01-01 00:00:00+00', '2026-01-01 00:00:00+00', 0, 0, true, true, 'system_InboundAudioFailureCorrectionAutomatedTask');