-- v3 initial version, as retention specific script
-- v4 rename case_retention_audit to case_retention_audit
--    add created* and last_modified* to case_retention
--    remove manual_override_retention
--    add comments and submitted_ts to case_retention
--    remove last* from case_retention
--    add legacy object_id fields to case_retention and retention_policy_type
--    change retention field total_sentence from integer to character varying
--    change sentence_name to policy_name on retention_policy_type
-- v5 add is_manual_override to case_retention
--    add event_ts to case_management_retention
--    amend eve_id on case_managment_retention to be nullable
-- v6 change submitted_ts to submitted_by on case_retention and FK for same, and missing one on created_by
--    add last_modified* to case_retention and FK 
-- v7 change retention_policy_type.duration from integer to character varying, to support nYnMnD format used elsewhere
--    amend retention_policy_type.policy_end_ts to nullable
--    amend total_sentence on case_management_retention & case_retention to nullable
--    add display_name and description to retention_policy_type
--    remove is_manual_override  from case_retention
--    amend fixed_policy_key from integer to character varying
--    amend eve_id on case_management_retention to not null
--    remove event_ts from case_management_retention 
-- v8 amend case_retention.retain_until_applied_on_ts to be nullable
-- v9 add confidence_category to case_retention
-- v10 switch all tablespaces to pg_default
-- v11 add table case_retention_audit_heritage
-- v12 add dm_sysobject_s attributes to case_retention_audit_heritage
-- v13 add table retention_confidence_category_mapper


SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

--List of Table Aliases
-- case_management_retention            CMR
-- case_retention                       CAR  
-- retention_confidence_category_mapper RCC
-- retention_policy_type                RPT


CREATE TABLE case_management_retention
(cmr_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,rpt_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       NOT NULL                
,total_sentence              CHARACTER VARYING                       -- < is this integer or the nYnMnD >
) TABLESPACE pg_default;

CREATE TABLE case_retention
(car_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,rpt_id                      INTEGER                       NOT NULL
,cmr_id                      INTEGER            
,total_sentence              CHARACTER VARYING                       -- < is this integer or the nYnMnD >
,retain_until_ts             TIMESTAMP WITH TIME ZONE      NOT NULL 
,retain_until_applied_on_ts  TIMESTAMP WITH TIME ZONE       
,current_state               CHARACTER VARYING             NOT NULL  -- can we agree on single chars, eg P-pending, E-expired, A-active
,comments                    CHARACTER VARYING 
,confidence_category         INTEGER                                 -- no FK to RCC.confidence_category
,retention_object_id         CHARACTER VARYING                       -- PK of legacy source migration table 
,submitted_by                INTEGER                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

CREATE TABLE case_retention_audit_heritage
(r_object_id                 CHARACTER VARYING(16)         NOT NULL
,i_partition                 INTEGER
,c_case_id                   CHARACTER VARYING(32)         
,c_date_retention_amended    TIMESTAMP WITH TIME ZONE
,c_comments                  CHARACTER VARYING
,c_date_previous_retention   TIMESTAMP WITH TIME ZONE
,c_username                  CHARACTER VARYING(32)
,c_status                    CHARACTER VARYING(32)
,c_courthouse                CHARACTER VARYING(64)
,c_policy_type               CHARACTER VARYING(20)
,c_case_closed_date          TIMESTAMP WITH TIME ZONE
,object_name                 CHARACTER VARYING(255)
,r_creator_name              CHARACTER VARYING(32)
,r_creation_date             TIMESTAMP WITH TIME ZONE
,r_modifier                  CHARACTER VARYING(32)
,r_modify_date               TIMESTAMP WITH TIME ZONE
,owner_name                  CHARACTER VARYING(32)
) TABLESPACE pg_default;

CREATE TABLE retention_confidence_category_mapper
(rcc_id                      INTEGER                       NOT NULL
,ret_conf_score              INTEGER
,ret_conf_reason             CHARACTER VARYING
,confidence_category         INTEGER                                 -- effectively a master list of valid categories found on case_retention
,description                 CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;


CREATE TABLE retention_policy_type
(rpt_id                      INTEGER                       NOT NULL
,fixed_policy_key            CHARACTER VARYING             NOT NULL 
,policy_name                 CHARACTER VARYING             NOT NULL
,display_name                CHARACTER VARYING             NOT NULL
,duration                    CHARACTER VARYING             NOT NULL -- changed to accommodate nYnMnD
,policy_start_ts             TIMESTAMP WITH TIME ZONE      NOT NULL
,policy_end_ts               TIMESTAMP WITH TIME ZONE  
,description                 CHARACTER VARYING             NOT NULL  
,retention_policy_object_id  CHARACTER VARYING                      -- PK of legacy source migration table
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

CREATE UNIQUE INDEX case_management_retention_pk ON case_management_retention(cmr_id) TABLESPACE pg_default;
ALTER TABLE case_management_retention ADD PRIMARY KEY USING INDEX case_management_retention_pk;

CREATE UNIQUE INDEX case_retention_pk ON case_retention(car_id) TABLESPACE pg_default; 
ALTER TABLE case_retention            ADD PRIMARY KEY USING INDEX case_retention_pk;

CREATE UNIQUE INDEX retention_confidence_category_mapper_pk ON retention_confidence_category_mapper(rcc_id) TABLESPACE pg_default; 
ALTER TABLE retention_confidence_category_mapper     ADD PRIMARY KEY USING INDEX retention_confidence_category_mapper_pk;

CREATE UNIQUE INDEX retention_policy_type_pk ON retention_policy_type(rpt_id) TABLESPACE pg_default; 
ALTER TABLE retention_policy_type     ADD PRIMARY KEY USING INDEX retention_policy_type_pk;

CREATE SEQUENCE cmr_seq CACHE 20;
CREATE SEQUENCE car_seq CACHE 20;
CREATE SEQUENCE rcc_seq CACHE 20;
CREATE SEQUENCE rpt_seq CACHE 20;

ALTER TABLE case_retention            
ADD CONSTRAINT case_retention_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_retention           
ADD CONSTRAINT case_retention_retention_policy_type_fk
FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE case_retention             
ADD CONSTRAINT case_retention_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention             
ADD CONSTRAINT case_retention_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention             
ADD CONSTRAINT case_retention_submitted_by_fk
FOREIGN KEY (submitted_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention            
ADD CONSTRAINT case_retention_case_management_retention_fk
FOREIGN KEY (cmr_id) REFERENCES case_management_retention(cmr_id);

ALTER TABLE case_management_retention            
ADD CONSTRAINT case_management_retention_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_management_retention            
ADD CONSTRAINT case_management_retention_retention_policy_type_fk
FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE case_management_retention            
ADD CONSTRAINT case_management_retention_event_fk
FOREIGN KEY (eve_id) REFERENCES event(eve_id);

ALTER TABLE retention_confidence_category_mapper
ADD CONSTRAINT retention_confidence_category_mapper_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE retention_confidence_category_mapper
ADD CONSTRAINT retention_confidence_category_mapper_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE retention_policy_type             
ADD CONSTRAINT retention_policy_type_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE retention_policy_type             
ADD CONSTRAINT retention_policy_type_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);



GRANT SELECT,INSERT,UPDATE,DELETE ON case_management_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_confidence_category_mapper TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_policy_type TO darts_user;

GRANT SELECT,UPDATE ON  cmr_seq TO darts_user;
GRANT SELECT,UPDATE ON  car_seq TO darts_user;
GRANT SELECT,UPDATE ON  rcc_seq TO darts_user;
GRANT SELECT,UPDATE ON  rpt_seq TO darts_user;
