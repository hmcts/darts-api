delete from security_group_user_account_ae where usr_id in (1,2,3);

INSERT INTO darts.security_group_user_account_ae(usr_id, grp_id) VALUES (1, 10);--xhibit
INSERT INTO darts.security_group_user_account_ae(usr_id, grp_id) VALUES (2, 11);--cp
INSERT INTO darts.security_group_user_account_ae(usr_id, grp_id) VALUES (3, 12);--viq - Dar Pc
INSERT INTO darts.security_group_user_account_ae(usr_id, grp_id) VALUES (3, 13);--viq - Mid Tier
