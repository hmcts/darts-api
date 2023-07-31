CREATE UNIQUE INDEX security_group_pk               ON security_group(grp_id) TABLESPACE darts_indexes;
ALTER TABLE security_group                          ADD PRIMARY KEY USING INDEX security_group_pk;

CREATE UNIQUE INDEX security_group_membership_ae_pk ON security_group_membership_ae(usr_id,grp_id) TABLESPACE darts_indexes;
ALTER TABLE security_group_membership_ae            ADD PRIMARY KEY USING INDEX security_group_membership_ae_pk;

CREATE UNIQUE INDEX security_role_pk                ON security_role(rol_id) TABLESPACE darts_indexes;
ALTER TABLE security_role                           ADD PRIMARY KEY USING INDEX security_role_pk;

CREATE UNIQUE INDEX security_permission_pk          ON security_permission(per_id) TABLESPACE darts_indexes;
ALTER TABLE security_permission                     ADD PRIMARY KEY USING INDEX security_permission_pk;

CREATE UNIQUE INDEX security_role_permission_ae_pk  ON security_role_permission_ae(rol_id,per_id) TABLESPACE darts_indexes;
ALTER TABLE security_role_permission_ae             ADD PRIMARY KEY USING INDEX security_role_permission_ae_pk;

CREATE UNIQUE INDEX security_group_courthouse_ae_pk ON security_group_courthouse_ae(grp_id,cth_id) TABLESPACE darts_indexes;
ALTER TABLE security_group_courthouse_ae            ADD PRIMARY KEY USING INDEX security_group_courthouse_ae_pk;
