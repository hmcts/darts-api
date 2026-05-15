UPDATE automated_task
SET set task_description = 'Marks for deletion audio that is stored in inbound that has been successfully been stored in unstructured.'
WHERE task_name = 'InboundAudioDeleter';
