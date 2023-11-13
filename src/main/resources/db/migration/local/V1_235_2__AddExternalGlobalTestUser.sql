
INSERT INTO darts.user_account (usr_id, user_name, user_email_address, account_guid, is_system_user) values (-44, 'darts_global_test_user', 'darts.global.user@hmcts.net', '995b3db3-27ab-446f-89c8-0aab1901484b', true);

INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id) values (-44, -14);

UPDATE darts.security_group
SET global_access = true
WHERE group_name in ('Xhibit Group', 'Cpp Group');

