--Fix name in the automated_task table which was incorreclty set to outboundAudioDeleter
UPDATE user_account
SET user_name = 'system_InboundAudioDeleter'
WHERE usr_id = -9;

--Assign email addresses to the automated task users
update user_account
set user_email_address = user_name || '@hmcts.net'
where user_email_address is null
  and usr_id between -36 and -6
  and user_name like 'system_%';