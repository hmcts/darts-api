INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'SpikeDisableAndEnableInDeployedEnvironment',
        'Prototype find files job to verify disabled deployment and environment-specific enabling and disabling',
        '0 */5 * * * *', true, 1000, current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by,
                          is_system_user, is_active, user_full_name)
VALUES (-54, 'system_SpikeDisableAndEnableInDeployedEnvironmentAutomatedTask',
        'system_SpikeDisableAndEnableInDeployedEnvironmentAutomatedTask@hmcts.net',
        'system_SpikeDisableAndEnableInDeployedEnvironmentAutomatedTask', current_timestamp, current_timestamp, 0, 0,
        true, true, 'system_SpikeDisableAndEnableInDeployedEnvironmentAutomatedTask');
