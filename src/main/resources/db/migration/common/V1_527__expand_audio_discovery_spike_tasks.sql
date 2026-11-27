UPDATE automated_task
SET task_name = 'AudioSpikeCvpExternal', task_description = 'DMA-16 read-only external CVP blob listing',
    task_enabled = false, last_modified_ts = current_timestamp, last_modified_by = 0
WHERE task_name = 'AudioSpikeCvp';

UPDATE user_account
SET user_name = 'system_AudioSpikeCvpExternalAutomatedTask',
    user_email_address = 'system_AudioSpikeCvpExternalAutomatedTask@hmcts.net',
    user_full_name = 'system_AudioSpikeCvpExternalAutomatedTask', last_modified_ts = current_timestamp, last_modified_by = 0
WHERE user_email_address = 'system_AudioSpikeCvpAutomatedTask@hmcts.net';

INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'AudioSpikeCvp', 'DMA-16 List all inbound blobs without a prefix',
        '0 */10 * * * *', true, 1000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts,
                          last_modified_by, created_by, is_system_user, is_active, user_full_name)
VALUES (-58, 'system_AudioSpikeCvpAutomatedTask', 'system_AudioSpikeCvpAutomatedTask@hmcts.net', 'DMA-16 spike system user',
        current_timestamp, current_timestamp, 0, 0, true, true, 'system_AudioSpikeCvpAutomatedTask');

INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'AudioSpikeCombinedFaulty', 'DMA-16 List nonexistent CVP container then CAR to demonstrate failure coupling',
        '0 */10 * * * *', true, 1000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts,
                          last_modified_by, created_by, is_system_user, is_active, user_full_name)
VALUES (-59, 'system_AudioSpikeCombinedFaultyAutomatedTask', 'system_AudioSpikeCombinedFaultyAutomatedTask@hmcts.net', 'DMA-16 spike system user',
        current_timestamp, current_timestamp, 0, 0, true, true, 'system_AudioSpikeCombinedFaultyAutomatedTask');

