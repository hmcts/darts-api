INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable)
VALUES (6, 'InboundToUnstructuredDataStore', 'Move Inbound files to Unstructured data store', '0 0/5 * * * *', true);

ALTER SEQUENCE aut_seq RESTART WITH 7;
