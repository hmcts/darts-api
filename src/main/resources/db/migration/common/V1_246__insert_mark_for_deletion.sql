INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable)
VALUES (7, 'UnstructuredAudioDeleter', 'Marks data for deletion in unstructured data stores that have been in ARM for a set time', '0 0 22 * * *', true);

ALTER SEQUENCE aut_seq RESTART WITH 8;
