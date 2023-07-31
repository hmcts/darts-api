--V13 change name of security_group_membership_ae to security_group_user_account_ae
--    added NOT NULL to all PK columns and "name" columns
--    added tablespaces to table creation (index ones already existed)

-- assuming this already exists:
-- CREATE TABLESPACE darts_tables  location 'E:/PostgreSQL/Tables';
-- CREATE TABLESPACE darts_indexes location 'E:/PostgreSQL/Indexes';

-- GRANT ALL ON TABLESPACE darts_tables TO darts_owner;
-- GRANT ALL ON TABLESPACE darts_indexes TO darts_owner;

-- List of Table Aliases

--security_group                       GRP
--security_group_user_account_ae       GUA
--security_role                        ROL
--security_permission                  PER
--security_role_permission_ae          ROP
--security_group_courthouse_ae         GRC

-- SET ROLE DARTS_OWNER;
-- SET SEARCH_PATH TO darts;

CREATE TABLE security_group
(grp_id                  INTEGER                         NOT NULL
,rol_id                  INTEGER                         NOT NULL
,r_dm_group_s_object_id  CHARACTER VARYING(16)
,group_name              CHARACTER VARYING               NOT NULL
,is_private              BOOLEAN
,description             CHARACTER VARYING
,r_modify_date           TIMESTAMP WITH TIME ZONE
,group_class             CHARACTER VARYING
,group_global_unique_id  CHARACTER VARYING
,group_display_name      CHARACTER VARYING
);

COMMENT ON TABLE security_group
IS 'migration columns all sourced directly from dm_group_s, additional attributes may be required from dm_user_s, but data only where dm_user_s.r_is_group=1';
COMMENT ON COLUMN security_group.grp_id
IS 'primary key of security_group';
COMMENT ON COLUMN security_group.r_dm_group_s_object_id
IS 'internal Documentum primary key from dm_group_s';

CREATE TABLE security_group_user_account_ae
(usr_id                 INTEGER                         NOT NULL
,grp_id                 INTEGER                         NOT NULL
);

COMMENT ON TABLE security_group_user_account_ae
IS 'is the associative entity mapping users to groups, content will be defined by dm_group_r';
COMMENT ON COLUMN security_group_user_account_ae.usr_id
IS 'foreign key from user_account';
COMMENT ON COLUMN security_group_user_account_ae.grp_id
IS 'foreign key from security_group';



CREATE TABLE security_role
(rol_id                  INTEGER                         NOT NULL
,role_name               CHARACTER VARYING               NOT NULL
);

CREATE TABLE security_permission
(per_id                  INTEGER                         NOT NULL
,permission_name         CHARACTER VARYING               NOT NULL
);

CREATE TABLE security_role_permission_ae
(rol_id                  INTEGER                         NOT NULL
,per_id                  INTEGER                         NOT NULL
);

CREATE TABLE security_group_courthouse_ae
(grp_id                  INTEGER                         NOT NULL
,cth_id                  INTEGER                         NOT NULL
);


CREATE SEQUENCE grp_seq CACHE 20;
CREATE SEQUENCE rol_seq CACHE 20;
CREATE SEQUENCE per_seq CACHE 20;

