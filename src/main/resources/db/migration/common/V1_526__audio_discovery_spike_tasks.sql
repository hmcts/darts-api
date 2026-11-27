INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'AudioSpikeCvp', 'DMA-16 read-only blob discovery comparison: Cvp',
        '0 */10 * * * *', true, 1000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts,
                          last_modified_by, created_by, is_system_user, is_active, user_full_name)
VALUES (-55, 'system_AudioSpikeCvpAutomatedTask', 'system_AudioSpikeCvpAutomatedTask@hmcts.net', 'DMA-16 spike system user',
        current_timestamp, current_timestamp, 0, 0, true, true, 'system_AudioSpikeCvpAutomatedTask');

INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'AudioSpikeCar', 'DMA-16 read-only blob discovery comparison: Car',
        '0 */10 * * * *', true, 1000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts,
                          last_modified_by, created_by, is_system_user, is_active, user_full_name)
VALUES (-56, 'system_AudioSpikeCarAutomatedTask', 'system_AudioSpikeCarAutomatedTask@hmcts.net', 'DMA-16 spike system user',
        current_timestamp, current_timestamp, 0, 0, true, true, 'system_AudioSpikeCarAutomatedTask');

INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'AudioSpikeCombined', 'DMA-16 read-only blob discovery comparison: Combined',
        '0 */10 * * * *', true, 1000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts,
                          last_modified_by, created_by, is_system_user, is_active, user_full_name)
VALUES (-57, 'system_AudioSpikeCombinedAutomatedTask', 'system_AudioSpikeCombinedAutomatedTask@hmcts.net', 'DMA-16 spike system user',
        current_timestamp, current_timestamp, 0, 0, true, true, 'system_AudioSpikeCombinedAutomatedTask');
