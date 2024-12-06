--V13 change name of security_group_membership_ae to security_group_user_account_ae
--    added NOT NULL to all PK columns and "name" columns
--    added tablespaces to table creation (index ones already existed)
--V14 added following columns to all tables (apart from ae tables - not supported by hibernate) and FKs to user_account
--        ,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
--        ,created_by                  INTEGER                       NOT NULL
--        ,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
--        ,last_modified_by            INTEGER                       NOT NULL  
--     moved INSERT statements to new file security_test_data.sql
--v15 comment preceding lines 
--v16 removing created* & last_modified* from SECURITY_ROLE, SECURITY_PERMISSION
--v17 add display_name and display_state to security_role 
--    add global_access and display_state to security_group
--v18 add use_interpreter boolean on security_group 
--    add defaults to global_access and use_interpreter to false  
--v19 remove r_ from dm_group_s_object_id and r_modify_date  from security_group
--    add display_name to security_group
--v20 remove group_class and group_display_name from security_group
--v21 add unique index on role_name to security_role
--    add unique index on group_name to security_group
--v22 change name of FK on security_group last_modified
--v23 switch all tablespaces to pg_default
--v24 add group_display_name to security_group, for direct mapping from legacy

-- assuming this already exists:
-- CREATE TABLESPACE pg_default  location 'E:/PostgreSQL/Tables';
-- CREATE TABLESPACE pg_default location 'E:/PostgreSQL/Indexes';

-- GRANT ALL ON TABLESPACE pg_default TO darts_owner;
-- GRANT ALL ON TABLESPACE pg_default TO darts_owner;

-- List of Table Aliases

--security_group                       GRP
--security_group_courthouse_ae         GRC
--security_group_user_account_ae       GUA
--security_permission                  PER
--security_role                        ROL
--security_role_permission_ae          ROP

SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

CREATE TABLE security_group
(grp_id                  INTEGER                         NOT NULL
,rol_id                  INTEGER                         NOT NULL
,global_access           BOOLEAN                         NOT NULL  DEFAULT FALSE
,display_state           BOOLEAN                         NOT NULL
,display_name            CHARACTER VARYING               NOT NULL
,dm_group_s_object_id    CHARACTER VARYING(16)
,group_name              CHARACTER VARYING               NOT NULL
,group_display_name      CHARACTER VARYING
,is_private              BOOLEAN
,description             CHARACTER VARYING
,group_global_unique_id  CHARACTER VARYING
,use_interpreter         BOOLEAN                         NOT NULL  DEFAULT FALSE
,created_ts              TIMESTAMP WITH TIME ZONE        NOT NULL
,created_by              INTEGER                         NOT NULL
,last_modified_ts        TIMESTAMP WITH TIME ZONE        NOT NULL
,last_modified_by        INTEGER                         NOT NULL  
) TABLESPACE pg_default;

COMMENT ON TABLE security_group 
IS 'migration columns all sourced directly from dm_group_s, additional attributes may be required from dm_user_s, but data only where dm_user_s.r_is_group=1';
COMMENT ON COLUMN security_group.grp_id
IS 'primary key of security_group';
COMMENT ON COLUMN security_group.dm_group_s_object_id
IS 'internal Documentum primary key from dm_group_s';
COMMENT ON COLUMN security_group.group_display_name
IS 'derived directly from dm_group_s.group_display_name';
COMMENT ON COLUMN security_group.display_name
IS 'for purpose of providing a display_name for modernised solution';

CREATE TABLE security_group_user_account_ae
(usr_id                 INTEGER                         NOT NULL
,grp_id                 INTEGER                         NOT NULL
) TABLESPACE pg_default;

COMMENT ON TABLE security_group_user_account_ae 
IS 'is the associative entity mapping users to groups, content will be defined by dm_group_r';
COMMENT ON COLUMN security_group_user_account_ae.usr_id
IS 'foreign key from user_account';
COMMENT ON COLUMN security_group_user_account_ae.grp_id
IS 'foreign key from security_group';



CREATE TABLE security_role
(rol_id                  INTEGER                         NOT NULL
,role_name               CHARACTER VARYING               NOT NULL
,display_name            CHARACTER VARYING               NOT NULL
,display_state           BOOLEAN                         NOT NULL
) TABLESPACE pg_default;

CREATE TABLE security_permission
(per_id                  INTEGER                         NOT NULL
,permission_name         CHARACTER VARYING               NOT NULL
) TABLESPACE pg_default;

CREATE TABLE security_role_permission_ae
(rol_id                  INTEGER                         NOT NULL
,per_id                  INTEGER                         NOT NULL
) TABLESPACE pg_default;

CREATE TABLE security_group_courthouse_ae
(grp_id                  INTEGER                         NOT NULL
,cth_id                  INTEGER                         NOT NULL
) TABLESPACE pg_default;

CREATE UNIQUE INDEX security_group_pk               ON security_group(grp_id) TABLESPACE pg_default; 
ALTER TABLE security_group                          ADD PRIMARY KEY USING INDEX security_group_pk;

CREATE UNIQUE INDEX security_group_user_account_ae_pk ON security_group_user_account_ae(usr_id,grp_id) TABLESPACE pg_default;
ALTER TABLE security_group_user_account_ae            ADD PRIMARY KEY USING INDEX security_group_user_account_ae_pk;

CREATE UNIQUE INDEX security_role_pk                ON security_role(rol_id) TABLESPACE pg_default; 
ALTER TABLE security_role                           ADD PRIMARY KEY USING INDEX security_role_pk;

CREATE UNIQUE INDEX security_permission_pk          ON security_permission(per_id) TABLESPACE pg_default;
ALTER TABLE security_permission                     ADD PRIMARY KEY USING INDEX security_permission_pk;

CREATE UNIQUE INDEX security_role_permission_ae_pk  ON security_role_permission_ae(rol_id,per_id) TABLESPACE pg_default;
ALTER TABLE security_role_permission_ae             ADD PRIMARY KEY USING INDEX security_role_permission_ae_pk;

CREATE UNIQUE INDEX security_group_courthouse_ae_pk ON security_group_courthouse_ae(grp_id,cth_id) TABLESPACE pg_default;
ALTER TABLE security_group_courthouse_ae            ADD PRIMARY KEY USING INDEX security_group_courthouse_ae_pk;


CREATE SEQUENCE grp_seq CACHE 20;
CREATE SEQUENCE rol_seq CACHE 20;
CREATE SEQUENCE per_seq CACHE 20;

ALTER TABLE security_group                    
ADD CONSTRAINT security_group_role_fk
FOREIGN KEY (rol_id) REFERENCES security_role(rol_id);

ALTER TABLE security_group
ADD CONSTRAINT security_group_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE security_group
ADD CONSTRAINT security_group_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);



ALTER TABLE security_group_courthouse_ae         
ADD CONSTRAINT security_group_courthouse_group_fk
FOREIGN KEY (grp_id) REFERENCES security_group(grp_id);

ALTER TABLE security_group_courthouse_ae         
ADD CONSTRAINT security_group_courthouse_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);


ALTER TABLE security_group_user_account_ae         
ADD CONSTRAINT security_group_user_account_ae_user_fk
FOREIGN KEY (usr_id) REFERENCES user_account(usr_id);

ALTER TABLE security_group_user_account_ae         
ADD CONSTRAINT security_group_user_account_ae_group_fk
FOREIGN KEY (grp_id) REFERENCES security_group(grp_id);


ALTER TABLE security_role_permission_ae          
ADD CONSTRAINT security_role_permission_ae_role_fk
FOREIGN KEY (rol_id) REFERENCES security_role(rol_id);

ALTER TABLE security_role_permission_ae          
ADD CONSTRAINT security_role_permission_permission_fk
FOREIGN KEY (per_id) REFERENCES security_permission(per_id);

-- additional UNIQUE constraints

CREATE UNIQUE INDEX rol_rol_nm_idx       ON security_role(role_name) TABLESPACE pg_default; 
CREATE UNIQUE INDEX grp_grp_nm_idx       ON security_group(group_name) TABLESPACE pg_default; 

