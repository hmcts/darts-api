CREATE TABLE security_group
(grp_id                  INTEGER                    NOT NULL
,rol_id                  INTEGER                    NOT NULL
,r_dm_group_s_object_id  CHARACTER VARYING(16)
,group_name              CHARACTER VARYING          NOT NULL
,is_private              BOOLEAN
,description             CHARACTER VARYING
,r_modify_date           TIMESTAMP WITH TIME ZONE
,group_class             CHARACTER VARYING
,group_global_unique_id  CHARACTER VARYING
,group_display_name      CHARACTER VARYING
);

CREATE TABLE security_group_membership_ae
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

CREATE TABLE security_role_permission_ae
(rol_id                  INTEGER                    NOT NULL
,per_id                  INTEGER                    NOT NULL
);

CREATE TABLE security_group_courthouse_ae
(grp_id                  INTEGER                    NOT NULL
,cth_id                  INTEGER                    NOT NULL
);

CREATE SEQUENCE grp_seq CACHE 20;
CREATE SEQUENCE rol_seq CACHE 20;
CREATE SEQUENCE per_seq CACHE 20;

