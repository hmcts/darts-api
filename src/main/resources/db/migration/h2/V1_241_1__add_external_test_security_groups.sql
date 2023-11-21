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

