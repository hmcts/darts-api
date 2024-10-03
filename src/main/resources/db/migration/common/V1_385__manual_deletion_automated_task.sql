INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                                  created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aut_seq'), 'ManualDeletion', 'Automated task to delete data marked for manual deletion.',
        '0 0 9 30 2 *', --(At 09:00 AM, on day 30 of the month, only in February)
        false, null, current_timestamp, 0, current_timestamp, 0);
