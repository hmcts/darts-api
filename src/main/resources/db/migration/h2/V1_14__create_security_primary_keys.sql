ALTER TABLE security_group ADD CONSTRAINT security_group_pk PRIMARY KEY (grp_id);

ALTER TABLE security_group_user_account_ae ADD CONSTRAINT security_group_user_account_ae_pk PRIMARY KEY (usr_id,grp_id);

ALTER TABLE security_role ADD CONSTRAINT security_role_pk PRIMARY KEY (rol_id);

ALTER TABLE security_permission ADD CONSTRAINT security_permission_pk PRIMARY KEY (per_id);

ALTER TABLE security_role_permission_ae ADD CONSTRAINT security_role_permission_ae_pk PRIMARY KEY (rol_id,per_id);

ALTER TABLE security_group_courthouse_ae ADD CONSTRAINT security_group_courthouse_ae_pk PRIMARY KEY (grp_id,cth_id);
