INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (8,'UnstructuredToArmDataStore','Move files from Unstructured to ARM data store','0 0/5 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 9;

INSERT INTO object_record_status (ors_id,ors_description) VALUES (12,'Arm Ingestion');
ALTER SEQUENCE ors_seq RESTART WITH 12;
