CREATE TABLE security_group
(grp_id                  INTEGER                    NOT NULL
,rol_id                  INTEGER
,r_dm_group_s_object_id  CHARACTER VARYING(16)
,group_name              CHARACTER VARYING
,is_private              BOOLEAN
,description             CHARACTER VARYING
,r_modify_date           TIMESTAMP WITH TIME ZONE
,group_class             CHARACTER VARYING
,group_global_unique_id  CHARACTER VARYING
,group_display_name      CHARACTER VARYING
);

CREATE TABLE security_group_membership
(usr_id                  INTEGER                    NOT NULL
,grp_id                  INTEGER                    NOT NULL
);

CREATE TABLE security_role
(rol_id                  INTEGER                    NOT NULL
,role_name               CHARACTER VARYING          NOT NULL
);

CREATE TABLE security_permission
(per_id                  INTEGER                    NOT NULL
,permission_name         CHARACTER VARYING          NOT NULL
);

CREATE TABLE security_role_permission
(rol_id                  INTEGER                    NOT NULL
,per_id                  INTEGER                    NOT NULL
);

CREATE TABLE security_group_courthouse
(grp_id                  INTEGER                    NOT NULL
,cth_id                  INTEGER                    NOT NULL
);

ALTER TABLE security_group                    ADD PRIMARY KEY(grp_id);
ALTER TABLE security_role                     ADD PRIMARY KEY(rol_id);
ALTER TABLE security_permission               ADD PRIMARY KEY(per_id);

ALTER TABLE security_group_membership         ADD CONSTRAINT group_membership_user_fk
FOREIGN KEY (usr_id) REFERENCES user_account(usr_id);
ALTER TABLE security_group_membership         ADD CONSTRAINT group_membership_group_fk
FOREIGN KEY (grp_id) REFERENCES security_group(grp_id);

ALTER TABLE security_group                    ADD CONSTRAINT group_role_fk
FOREIGN KEY (rol_id) REFERENCES security_role(rol_id);

ALTER TABLE security_role_permission          ADD CONSTRAINT role_permission_role_fk
FOREIGN KEY (rol_id) REFERENCES security_role(rol_id);
ALTER TABLE security_role_permission          ADD CONSTRAINT permission_permission_fk
FOREIGN KEY (per_id) REFERENCES security_permission(per_id);

ALTER TABLE security_group_courthouse         ADD CONSTRAINT group_courthouse_group_fk
FOREIGN KEY (grp_id) REFERENCES security_group(grp_id);
ALTER TABLE security_group_courthouse         ADD CONSTRAINT group_courthouse_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);
