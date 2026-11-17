INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by, task_enabled)
VALUES (nextval('aut_seq'), 'FindFilesFromCVPAndCAR',
        'Find files from the CVP and CAR containers in the cvp-recordings blob store, store their location, and record the last successful run time',
        '0 */10 * * * *', true, 1000,
        current_timestamp, 0, current_timestamp, 0, false);

INSERT INTO user_account (usr_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_modified_by, created_by, is_system_user,
                          is_active, user_full_name)
VALUES (-54, 'system_FindFilesFromCVPAndCARAutomatedTask', 'system_FindFilesFromCVPAndCARAutomatedTask@hmcts.net', 'system_FindFilesFromCVPAndCARAutomatedTask',
        current_timestamp, current_timestamp, 0, 0, true, true, 'system_FindFilesFromCVPAndCARAutomatedTask');
