UPDATE automated_task
SET task_name = 'FindFilesFromCVPAndCAR',
    task_description = 'Find files from the CVP and CAR containers in the cvp-recordings blob store, store their location, and record the last successful run time',
    last_modified_ts = current_timestamp,
    last_modified_by = 0
WHERE task_name = 'FindFiles';

UPDATE user_account
SET user_name = 'system_FindFilesFromCVPAndCARAutomatedTask',
    user_email_address = 'system_FindFilesFromCVPAndCARAutomatedTask@hmcts.net',
    description = 'system_FindFilesFromCVPAndCARAutomatedTask',
    user_full_name = 'system_FindFilesFromCVPAndCARAutomatedTask',
    last_modified_ts = current_timestamp,
    last_modified_by = 0
WHERE usr_id = -54
  AND user_email_address = 'system_FindFilesAutomatedTask@hmcts.net';
