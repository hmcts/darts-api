INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (4,'InboundToUnstructuredDataStore','Move Inbound files to Unstructured data store','0 0/1 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 5;
