INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (4,'InboundAudioDeleter','Marks for deletion audio that is stored in outbound that has been successfully uploaded to ARM.','0 4 22 * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 5;

insert into user_account(usr_id,user_name,description,user_state, account_guid, is_system_user)
values (nextval('usr_seq'),'system_housekeeping','Housekeeping job', 1, 'ecfd1f14-c9b6-4f15-94c7-cc60e53f2c7a', true);


--Fixing incorrect OutboundAudioDeleter cron
update darts.automated_task
set cron_expression = '0 3 22 * * *'
where aut_id = 3;

