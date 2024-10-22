INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                                  created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (321, 'ProcessARMRPOPending', 'Update the record to STORED which has a status of ARM_RPO_PENDING and data_ingestion_ts is more than 24 hours',
        '0 16 23 * * *', --(16 mins past 11pm every day)
        false, 100000, current_timestamp, 0, current_timestamp, 0);
ALTER SEQUENCE aut_seq RESTART WITH 1000;