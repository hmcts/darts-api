UPDATE darts.user_account
SET account_guid = '78567440-d51c-4665-8664-218bd578dc68', is_system_user = true, user_name = 'cpp_user'
WHERE usr_id = -41;

UPDATE darts.user_account
SET account_guid = 'd040e413-f1ac-4902-b347-369f6e582559', is_system_user = true, user_name = 'xhibit_user'
WHERE usr_id = -40;


INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-46, -17);
INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-46, -16);
INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-46, -15);

INSERT INTO darts.user_account
(usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, created_ts, last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user)
VALUES(-49, NULL, 'darpc_midtier_user', 'darpc_midtier_user@hmcts.net', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'b884e22d-a38c-4a34-94d0-af7f0e177bc6', true);

INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-49, -17);

INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-49, -16);
