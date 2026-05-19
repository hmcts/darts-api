UPDATE automated_task
SET task_description = 'Marks for deletion audio that is stored in outbound that was last accessed a certain number of days ago.'
WHERE task_name = 'OutboundAudioDeleter';
