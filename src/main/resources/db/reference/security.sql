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

SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

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
) TABLESPACE darts_tables;

COMMENT ON TABLE security_group
IS 'migration columns all sourced directly from dm_group_s, additional attributes may be required from dm_user_s, but data only where dm_user_s.r_is_group=1';
COMMENT ON COLUMN security_group.grp_id
IS 'primary key of security_group';
COMMENT ON COLUMN security_group.r_dm_group_s_object_id
IS 'internal Documentum primary key from dm_group_s';

CREATE TABLE security_group_user_account_ae
(usr_id                 INTEGER                         NOT NULL
,grp_id                 INTEGER                         NOT NULL
) TABLESPACE darts_tables;

COMMENT ON TABLE security_group_user_account_ae
IS 'is the associative entity mapping users to groups, content will be defined by dm_group_r';
COMMENT ON COLUMN security_group_user_account_ae.usr_id
IS 'foreign key from user_account';
COMMENT ON COLUMN security_group_user_account_ae.grp_id
IS 'foreign key from security_group';



CREATE TABLE security_role
(rol_id                  INTEGER                         NOT NULL
,role_name               CHARACTER VARYING               NOT NULL
) TABLESPACE darts_tables;

CREATE TABLE security_permission
(per_id                  INTEGER                         NOT NULL
,permission_name         CHARACTER VARYING               NOT NULL
) TABLESPACE darts_tables;

CREATE TABLE security_role_permission_ae
(rol_id                  INTEGER                         NOT NULL
,per_id                  INTEGER                         NOT NULL
) TABLESPACE darts_tables;

CREATE TABLE security_group_courthouse_ae
(grp_id                  INTEGER                         NOT NULL
,cth_id                  INTEGER                         NOT NULL
) TABLESPACE darts_tables;

CREATE UNIQUE INDEX security_group_pk               ON security_group(grp_id) TABLESPACE darts_indexes;
ALTER TABLE security_group                          ADD PRIMARY KEY USING INDEX security_group_pk;

CREATE UNIQUE INDEX security_group_user_account_ae_pk ON security_group_user_account_ae(usr_id,grp_id) TABLESPACE darts_indexes;
ALTER TABLE security_group_user_account_ae            ADD PRIMARY KEY USING INDEX security_group_user_account_ae_pk;

CREATE UNIQUE INDEX security_role_pk                ON security_role(rol_id) TABLESPACE darts_indexes;
ALTER TABLE security_role                           ADD PRIMARY KEY USING INDEX security_role_pk;

CREATE UNIQUE INDEX security_permission_pk          ON security_permission(per_id) TABLESPACE darts_indexes;
ALTER TABLE security_permission                     ADD PRIMARY KEY USING INDEX security_permission_pk;

CREATE UNIQUE INDEX security_role_permission_ae_pk  ON security_role_permission_ae(rol_id,per_id) TABLESPACE darts_indexes;
ALTER TABLE security_role_permission_ae             ADD PRIMARY KEY USING INDEX security_role_permission_ae_pk;

CREATE UNIQUE INDEX security_group_courthouse_ae_pk ON security_group_courthouse_ae(grp_id,cth_id) TABLESPACE darts_indexes;
ALTER TABLE security_group_courthouse_ae            ADD PRIMARY KEY USING INDEX security_group_courthouse_ae_pk;


CREATE SEQUENCE grp_seq CACHE 20;
CREATE SEQUENCE rol_seq CACHE 20;
CREATE SEQUENCE per_seq CACHE 20;

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

delete from user_account;
insert into user_account (usr_id,user_name) values (1,'Brodie Mcknight');
insert into user_account (usr_id,user_name) values (2,'Jodie Fischer');
insert into user_account (usr_id,user_name) values (3,'Elspeth Villa');
insert into user_account (usr_id,user_name) values (4,'Helena Horton');
insert into user_account (usr_id,user_name) values (5,'Yunus Stein');
insert into user_account (usr_id,user_name) values (6,'Rehan Webster');
insert into user_account (usr_id,user_name) values (7,'Judy Sheindlin');
insert into user_account (usr_id,user_name) values (8,'Rafael Hatfield');
insert into user_account (usr_id,user_name) values (9,'Bridget York');
insert into user_account (usr_id,user_name) values (10,'Roy Stuart');
insert into user_account (usr_id,user_name) values (11,'Tom Denning');
insert into user_account (usr_id,user_name) values (12,'Google Translate');
insert into user_account (usr_id,user_name) values (13,'Maisey Mcdonald');
insert into user_account (usr_id,user_name) values (14,'Joe Dredd');
insert into user_account (usr_id,user_name) values (15,'Arthur Vandelay');
insert into user_account (usr_id,user_name) values (16,'Dale Barnett');
insert into user_account (usr_id,user_name) values (17,'James Wright');
insert into user_account (usr_id,user_name) values (18,'Lucy Amenuensis');
insert into user_account (usr_id,user_name) values (19,'Constance Harm');
insert into user_account (usr_id,user_name) values (20,'Paul Scrivener');
insert into user_account (usr_id,user_name) values (21,'Doc Hudson');
insert into user_account (usr_id,user_name) values (22,'Babel Fish');
insert into user_account (usr_id,user_name) values (23,'Ronnie Parks');
insert into user_account (usr_id,user_name) values (24,'Ling Woo');
insert into user_account (usr_id,user_name) values (25,'Rosa Mcbride');
insert into user_account (usr_id,user_name) values (26,'Ciara Erickson');
insert into user_account (usr_id,user_name) values (27,'Tessa Bowen');
insert into user_account (usr_id,user_name) values (28,'Kayne Owen');
insert into user_account (usr_id,user_name) values (29,'Natasha Wong');
insert into user_account (usr_id,user_name) values (30,'Greta Carillo');

insert into security_permission ( per_id,permission_name) values (1,'Accept Transcription Job Request');
insert into security_permission ( per_id,permission_name) values (2,'Approve/Reject Transcription Request');
insert into security_permission ( per_id,permission_name) values (3,'Listen to Audio for Download');
insert into security_permission ( per_id,permission_name) values (4,'Listen to Audio for Playback');
insert into security_permission ( per_id,permission_name) values (5,'Read Judges Notes');
insert into security_permission ( per_id,permission_name) values (6,'Read Transcribed Document');
insert into security_permission ( per_id,permission_name) values (7,'Request Audio');
insert into security_permission ( per_id,permission_name) values (8,'Request Transcription');
insert into security_permission ( per_id,permission_name) values (9,'Retention administration');
insert into security_permission ( per_id,permission_name) values (10,'Search Cases');
insert into security_permission ( per_id,permission_name) values (11,'Upload Judges Notes');
insert into security_permission ( per_id,permission_name) values (12,'Upload Transcription');
insert into security_permission ( per_id,permission_name) values (13,'View DARTS Inbox');
insert into security_permission ( per_id,permission_name) values (14,'View My Audios');
insert into security_permission ( per_id,permission_name) values (15,'View My Transcriptions');

insert into security_role( rol_id,role_name) values (1,'court manager');
insert into security_role( rol_id,role_name) values (2,'court clerk');
insert into security_role( rol_id,role_name) values (3,'judge');
insert into security_role( rol_id,role_name) values (4,'transcription company');
insert into security_role( rol_id,role_name) values (5,'language shop');

-- not dropping case or courthouse with each reload, so truncate content
delete from court_case;
delete from courthouse;

insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (1 ,'000','4 NEWTON STREET',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (2 ,'001','AMERSHAM LAW COURTS',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (3 ,'401','AYLESBURY',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (4 ,'002','BARNSTAPLE',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (5 ,'003','BARRONS COURT WALSALL',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (6 ,'004','BARROW IN FURNESS',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (7 ,'461','BASILDON',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (8 ,'404','BIRMINGHAM',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (9 ,'005','BIRMINGHAM LIBRARY AND REP',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (10,'428','BLACKFRIARS',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (11,'470','BOLTON',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (12,'406','BOURNEMOUTH',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (13,'402','BRADFORD',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (14,'006','BRIGHTON',current_timestamp,current_timestamp);
insert into courthouse(cth_id,courthouse_code,courthouse_name,created_ts,last_modified_ts) values (15,'408','BRISTOL',current_timestamp,current_timestamp);

insert into security_group(grp_id,rol_id,group_name) values ( 1, 4,'tc_transcription_co_1');
insert into security_group(grp_id,rol_id,group_name) values ( 2, 4,'tc_transcription_co_2');
insert into security_group(grp_id,rol_id,group_name) values ( 3, 4,'tc_transcription_co_3');
insert into security_group(grp_id,rol_id,group_name) values ( 4, 4,'tc_transcription_co_4');
insert into security_group(grp_id,rol_id,group_name) values ( 5, 4,'tc_transcription_co_5');

insert into security_group_courthouse_ae(grp_id,cth_id) values (1, 1);
insert into security_group_courthouse_ae(grp_id,cth_id) values (2, 2);
insert into security_group_courthouse_ae(grp_id,cth_id) values (2, 3);
insert into security_group_courthouse_ae(grp_id,cth_id) values (3, 4);
insert into security_group_courthouse_ae(grp_id,cth_id) values (3, 5);
insert into security_group_courthouse_ae(grp_id,cth_id) values (3, 6);
insert into security_group_courthouse_ae(grp_id,cth_id) values (4, 7);
insert into security_group_courthouse_ae(grp_id,cth_id) values (4, 8);
insert into security_group_courthouse_ae(grp_id,cth_id) values (4, 9);
insert into security_group_courthouse_ae(grp_id,cth_id) values (4,10);
insert into security_group_courthouse_ae(grp_id,cth_id) values (5,11);
insert into security_group_courthouse_ae(grp_id,cth_id) values (5,12);
insert into security_group_courthouse_ae(grp_id,cth_id) values (5,13);
insert into security_group_courthouse_ae(grp_id,cth_id) values (5,14);
insert into security_group_courthouse_ae(grp_id,cth_id) values (5,15);

insert into security_group(grp_id,rol_id,group_name) values ( 6,2,'moj_ch_4_newton_street_staff');
insert into security_group(grp_id,rol_id,group_name) values ( 7,2,'moj_ch_amersham_law_co_staff');
insert into security_group(grp_id,rol_id,group_name) values ( 8,2,'moj_ch_aylesbury_staff');
insert into security_group(grp_id,rol_id,group_name) values ( 9,2,'moj_ch_barnstaple_staff');
insert into security_group(grp_id,rol_id,group_name) values (10,2,'moj_ch_barrons_court_wa_staff');
insert into security_group(grp_id,rol_id,group_name) values (11,2,'moj_ch_barrow-in-furne_staff');
insert into security_group(grp_id,rol_id,group_name) values (12,2,'moj_ch_basildon_staff');
insert into security_group(grp_id,rol_id,group_name) values (13,2,'moj_ch_birmingham_staff');
insert into security_group(grp_id,rol_id,group_name) values (14,2,'moj_ch_birmingham_libr_staff');
insert into security_group(grp_id,rol_id,group_name) values (15,2,'moj_ch_blackfriars_staff');
insert into security_group(grp_id,rol_id,group_name) values (16,2,'moj_ch_bolton_staff');
insert into security_group(grp_id,rol_id,group_name) values (17,2,'moj_ch_bournemouth_staff');
insert into security_group(grp_id,rol_id,group_name) values (18,2,'moj_bradford_staff');
insert into security_group(grp_id,rol_id,group_name) values (19,2,'moj_brighton_staff');
insert into security_group(grp_id,rol_id,group_name) values (20,2,'moj_bristol_staff');

insert into security_group_courthouse_ae(grp_id,cth_id) values ( 6, 1);
insert into security_group_courthouse_ae(grp_id,cth_id) values ( 7, 2);
insert into security_group_courthouse_ae(grp_id,cth_id) values ( 8, 3);
insert into security_group_courthouse_ae(grp_id,cth_id) values ( 9, 4);
insert into security_group_courthouse_ae(grp_id,cth_id) values (10, 5);
insert into security_group_courthouse_ae(grp_id,cth_id) values (11, 6);
insert into security_group_courthouse_ae(grp_id,cth_id) values (12, 7);
insert into security_group_courthouse_ae(grp_id,cth_id) values (13, 8);
insert into security_group_courthouse_ae(grp_id,cth_id) values (14, 9);
insert into security_group_courthouse_ae(grp_id,cth_id) values (15,10);
insert into security_group_courthouse_ae(grp_id,cth_id) values (16,11);
insert into security_group_courthouse_ae(grp_id,cth_id) values (17,12);
insert into security_group_courthouse_ae(grp_id,cth_id) values (18,13);
insert into security_group_courthouse_ae(grp_id,cth_id) values (19,14);
insert into security_group_courthouse_ae(grp_id,cth_id) values (20,15);

insert into security_group(grp_id,rol_id,group_name) values (21, 1,'moj_ch_4_newton_street_appr');
insert into security_group(grp_id,rol_id,group_name) values (22, 1,'moj_ch_amersham_law_co_appr');
insert into security_group(grp_id,rol_id,group_name) values (23, 1,'moj_ch_aylesbury_appr');
insert into security_group(grp_id,rol_id,group_name) values (24, 1,'moj_ch_barnstaple_appr');
insert into security_group(grp_id,rol_id,group_name) values (25, 1,'moj_ch_barrons_court_wa_appr');
insert into security_group(grp_id,rol_id,group_name) values (26, 1,'moj_ch_barrow-in-furne_appr');
insert into security_group(grp_id,rol_id,group_name) values (27, 1,'moj_ch_basildon_appr');
insert into security_group(grp_id,rol_id,group_name) values (28, 1,'moj_ch_birmingham_appr');
insert into security_group(grp_id,rol_id,group_name) values (29, 1,'moj_ch_birmingham_libr_appr');
insert into security_group(grp_id,rol_id,group_name) values (30, 1,'moj_ch_blackfriars_appr');
insert into security_group(grp_id,rol_id,group_name) values (31, 1,'moj_ch_bolton_appr');
insert into security_group(grp_id,rol_id,group_name) values (32, 1,'moj_ch_bournemouth_appr');
insert into security_group(grp_id,rol_id,group_name) values (33, 1,'moj_ch_bradford_appr');
insert into security_group(grp_id,rol_id,group_name) values (34, 1,'moj_ch_brighton_appr');
insert into security_group(grp_id,rol_id,group_name) values (35, 1,'moj_ch_bristol_appr');

insert into security_group_courthouse_ae(grp_id,cth_id) values (21, 1);
insert into security_group_courthouse_ae(grp_id,cth_id) values (22, 2);
insert into security_group_courthouse_ae(grp_id,cth_id) values (23, 3);
insert into security_group_courthouse_ae(grp_id,cth_id) values (24, 4);
insert into security_group_courthouse_ae(grp_id,cth_id) values (25, 5);
insert into security_group_courthouse_ae(grp_id,cth_id) values (26, 6);
insert into security_group_courthouse_ae(grp_id,cth_id) values (27, 7);
insert into security_group_courthouse_ae(grp_id,cth_id) values (28, 8);
insert into security_group_courthouse_ae(grp_id,cth_id) values (29, 9);
insert into security_group_courthouse_ae(grp_id,cth_id) values (30,10);
insert into security_group_courthouse_ae(grp_id,cth_id) values (31,11);
insert into security_group_courthouse_ae(grp_id,cth_id) values (32,12);
insert into security_group_courthouse_ae(grp_id,cth_id) values (33,13);
insert into security_group_courthouse_ae(grp_id,cth_id) values (34,14);
insert into security_group_courthouse_ae(grp_id,cth_id) values (35,15);

insert into security_group(grp_id,rol_id,group_name) values (36,3,'moj_judges');

insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 1);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 2);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 3);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 4);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 5);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 6);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 7);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 8);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36, 9);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36,10);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36,11);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36,12);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36,13);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36,14);
insert into security_group_courthouse_ae(grp_id,cth_id) values (36,15);

insert into security_group(grp_id,rol_id,group_name) values (37,5,'moj_language_shop');

insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 1);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 2);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 3);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 4);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 5);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 6);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 7);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 8);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37, 9);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37,10);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37,11);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37,12);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37,13);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37,14);
insert into security_group_courthouse_ae(grp_id,cth_id) values (37,15);

insert into security_group_user_account_ae(usr_id,grp_id) values ( 1,12);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 1,13);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 2,22);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 2,23);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 3,26);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 4,27);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 5,27);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 6,28);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 6,29);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 6,21);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 8,13);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 8,14);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 8,28);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 9,16);
insert into security_group_user_account_ae(usr_id,grp_id) values (10,8);
insert into security_group_user_account_ae(usr_id,grp_id) values (10,9);
insert into security_group_user_account_ae(usr_id,grp_id) values (20,1);
insert into security_group_user_account_ae(usr_id,grp_id) values (18,5);
insert into security_group_user_account_ae(usr_id,grp_id) values (13,3);
insert into security_group_user_account_ae(usr_id,grp_id) values (17,2);
insert into security_group_user_account_ae(usr_id,grp_id) values (30,4);
insert into security_group_user_account_ae(usr_id,grp_id) values (12,37);
insert into security_group_user_account_ae(usr_id,grp_id) values (22,37);
insert into security_group_user_account_ae(usr_id,grp_id) values (24,36);
insert into security_group_user_account_ae(usr_id,grp_id) values (21,36);
insert into security_group_user_account_ae(usr_id,grp_id) values (19,36);
insert into security_group_user_account_ae(usr_id,grp_id) values (11,36);
insert into security_group_user_account_ae(usr_id,grp_id) values ( 7,36);
insert into security_group_user_account_ae(usr_id,grp_id) values (14,36);
insert into security_group_user_account_ae(usr_id,grp_id) values (15,1);
insert into security_group_user_account_ae(usr_id,grp_id) values (16,1);
insert into security_group_user_account_ae(usr_id,grp_id) values (23,1);
insert into security_group_user_account_ae(usr_id,grp_id) values (25,2);
insert into security_group_user_account_ae(usr_id,grp_id) values (26,2);
insert into security_group_user_account_ae(usr_id,grp_id) values (27,3);
insert into security_group_user_account_ae(usr_id,grp_id) values (28,3);
insert into security_group_user_account_ae(usr_id,grp_id) values (29,4);

insert into security_role_permission_ae(rol_id,per_id) values (1,2);
insert into security_role_permission_ae(rol_id,per_id) values (1,4);
insert into security_role_permission_ae(rol_id,per_id) values (1,6);
insert into security_role_permission_ae(rol_id,per_id) values (1,7);
insert into security_role_permission_ae(rol_id,per_id) values (1,8);
insert into security_role_permission_ae(rol_id,per_id) values (1,9);
insert into security_role_permission_ae(rol_id,per_id) values (1,10);
insert into security_role_permission_ae(rol_id,per_id) values (1,13);
insert into security_role_permission_ae(rol_id,per_id) values (1,14);
insert into security_role_permission_ae(rol_id,per_id) values (1,15);
insert into security_role_permission_ae(rol_id,per_id) values (2,4);
insert into security_role_permission_ae(rol_id,per_id) values (2,6);
insert into security_role_permission_ae(rol_id,per_id) values (2,7);
insert into security_role_permission_ae(rol_id,per_id) values (2,8);
insert into security_role_permission_ae(rol_id,per_id) values (2,9);
insert into security_role_permission_ae(rol_id,per_id) values (2,10);
insert into security_role_permission_ae(rol_id,per_id) values (2,13);
insert into security_role_permission_ae(rol_id,per_id) values (2,14);
insert into security_role_permission_ae(rol_id,per_id) values (2,15);
insert into security_role_permission_ae(rol_id,per_id) values (3,4);
insert into security_role_permission_ae(rol_id,per_id) values (3,5);
insert into security_role_permission_ae(rol_id,per_id) values (3,6);
insert into security_role_permission_ae(rol_id,per_id) values (3,7);
insert into security_role_permission_ae(rol_id,per_id) values (3,8);
insert into security_role_permission_ae(rol_id,per_id) values (3,9);
insert into security_role_permission_ae(rol_id,per_id) values (3,10);
insert into security_role_permission_ae(rol_id,per_id) values (3,11);
insert into security_role_permission_ae(rol_id,per_id) values (3,13);
insert into security_role_permission_ae(rol_id,per_id) values (3,14);
insert into security_role_permission_ae(rol_id,per_id) values (3,15);
insert into security_role_permission_ae(rol_id,per_id) values (4,1);
insert into security_role_permission_ae(rol_id,per_id) values (4,3);
insert into security_role_permission_ae(rol_id,per_id) values (4,6);
insert into security_role_permission_ae(rol_id,per_id) values (4,7);
insert into security_role_permission_ae(rol_id,per_id) values (4,10);
insert into security_role_permission_ae(rol_id,per_id) values (4,12);
insert into security_role_permission_ae(rol_id,per_id) values (4,13);
insert into security_role_permission_ae(rol_id,per_id) values (4,14);
insert into security_role_permission_ae(rol_id,per_id) values (4,15);
insert into security_role_permission_ae(rol_id,per_id) values (5,6);
insert into security_role_permission_ae(rol_id,per_id) values (5,7);
insert into security_role_permission_ae(rol_id,per_id) values (5,10);
insert into security_role_permission_ae(rol_id,per_id) values (5,13);
insert into security_role_permission_ae(rol_id,per_id) values (5,14);

insert into court_case(cas_id,cth_id,case_number,interpreter_used) values(1,1,'U01012023-00001',false);
insert into court_case(cas_id,cth_id,case_number,interpreter_used) values(2,1,'U01012023-00002',true);
insert into court_case(cas_id,cth_id,case_number,interpreter_used) values(3,1,'U01012023-00003',false);
insert into court_case(cas_id,cth_id,case_number,interpreter_used) values(4,2,'U01012023-00004',true);
insert into court_case(cas_id,cth_id,case_number,interpreter_used) values(5,2,'U01012023-00005',false);
insert into court_case(cas_id,cth_id,case_number,interpreter_used) values(6,3,'U01012023-00006',true);















