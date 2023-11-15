INSERT INTO darts.user_account (usr_id, user_name, user_email_address, account_guid, is_system_user) values (-40, 'Xhibit', 'xhibit@hmcts.net', '43cb3e21-b03a-4629-9908-b0ddaf7448b9', true);
INSERT INTO darts.user_account (usr_id, user_name, user_email_address, account_guid, is_system_user) values (-41, 'Cpp', 'cpp@hmcts.net', '43cb3e21-b03a-4629-9908-b0ddaf7448b9', true);

insert into darts.security_group(grp_id, rol_id,group_name, global_access) values (-14, 7, 'Xhibit Group', true);
insert into darts.security_group(grp_id, rol_id,group_name, global_access) values (-15, 8, 'Cpp Group', true);

UPDATE darts.user_account
SET account_guid = '43cb3e21-b03a-4629-9908-b0ddaf7448b9',
user_email_address = null
WHERE usr_id = '-999';
