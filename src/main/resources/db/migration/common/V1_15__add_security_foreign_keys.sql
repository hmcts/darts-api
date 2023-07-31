ALTER TABLE security_group_user_account_ae
ADD CONSTRAINT security_group_user_account_ae_user_fk
FOREIGN KEY (usr_id) REFERENCES user_account(usr_id);

ALTER TABLE security_group_user_account_ae
ADD CONSTRAINT security_group_user_account_ae_group_fk
FOREIGN KEY (grp_id) REFERENCES security_group(grp_id);

ALTER TABLE security_group
ADD CONSTRAINT security_group_role_fk
FOREIGN KEY (rol_id) REFERENCES security_role(rol_id);

ALTER TABLE security_role_permission_ae
ADD CONSTRAINT security_role_permission_ae_role_fk
FOREIGN KEY (rol_id) REFERENCES security_role(rol_id);

ALTER TABLE security_role_permission_ae
ADD CONSTRAINT security_role_permission_permission_fk
FOREIGN KEY (per_id) REFERENCES security_permission(per_id);

ALTER TABLE security_group_courthouse_ae
ADD CONSTRAINT security_group_courthouse_group_fk
FOREIGN KEY (grp_id) REFERENCES security_group(grp_id);

ALTER TABLE security_group_courthouse_ae
ADD CONSTRAINT security_group_courthouse_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);
