INSERT INTO darts.user_account (usr_id, user_name, user_email_address) values (-40, 'Xhibit', 'xhibit@hmcts.net');
INSERT INTO darts.user_account (usr_id, user_name, user_email_address) values (-41, 'Cpp', 'cpp@hmcts.net');
INSERT INTO darts.user_account (usr_id, user_name, user_email_address) values (-42, 'Dar Pc', 'dar.pc@hmcts.net');
INSERT INTO darts.user_account (usr_id, user_name, user_email_address) values (-43, 'Mid Tier', 'dar.midtier@hmcts.net');

insert into darts.security_group(grp_id,rol_id,group_name) values (-14,7,'Xhibit Group');
insert into darts.security_group(grp_id,rol_id,group_name) values (-15,8,'Cpp Group');
insert into darts.security_group(grp_id,rol_id,group_name) values (-16,9,'Dar Pc Group');
insert into darts.security_group(grp_id,rol_id,group_name) values (-17,10,'Mid Tier Group');

INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id) values (-40, -14);
INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id) values (-41, -15);
INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id) values (-42, -16);
INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id) values (-43, -17);

INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (-14, 1);
INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (-15, 1);
INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (-16, 1);
INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (-17, 1);

