INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable, batch_size)
VALUES (nextval('aut_seq'),'UnstructuredTranscriptionAnnotationDeleter','Marks for deletion the transcription and annotation that is stored in unstructured for the longer than a specific date.','0 0 22 * * *',true,50);