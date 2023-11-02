INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (3,'OutboundAudioDeleter','Marks for deletion audio that is stored in outbound that was last accessed a certain number of days a ago.','00 22 * * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 4;
