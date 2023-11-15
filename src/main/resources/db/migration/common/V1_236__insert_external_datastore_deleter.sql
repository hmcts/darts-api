INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (4,'ExternalDataStoreDeleter','Deletes data marked for deletion in inbound, unstructured, outbound datastores','00 22 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 5;
