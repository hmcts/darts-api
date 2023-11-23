insert into darts.security_group(grp_id, rol_id,group_name, global_access) values (-16, 9, 'Dar Pc Group', true);
insert into darts.security_group(grp_id, rol_id,group_name, global_access) values (-17, 10, 'Mid Tier Group', true);

INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-46, -17);
INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-46, -16);
INSERT INTO darts.security_group_user_account_ae
(usr_id, grp_id)
VALUES(-46, -15);

UPDATE darts.security_group
SET global_access = true
WHERE group_name in ('Dar Pc Group', 'Mid Tier Group');

UPDATE darts.user_account
SET is_system_user = true
WHERE user_name = 'system';

UPDATE darts.user_account
SET user_email_address = 'dartssystemuser@hmcts.net'
WHERE usr_id = 0;

