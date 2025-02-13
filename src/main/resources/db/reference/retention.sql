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
-- v14 add case_retention table, which is to store dmc_rps_retainer objects from legacy
--     add cas_id and rpt_id FKs to case_retention_audit_heritage
--     move case_overflow from main script to here
--     add is_current to case_retention_audit_heritage and case_rps_retainer
--     add numerous columns to case_overflow
--     add synthetic PK column to case_retention_audit_heritage
--     remove various columns from case_overflow to add to case_retention_extra
--     introduce case_retention_extra table
--     rename case_rps_retainer to_rps_retainer
-- v15 add FKs on rps_retainer for created/last_modified columns
--     normalise 4 user fields on case_retention_audit_heritage
--     amend rps_retainer.cas_id to nullable
--     add case_total_sentence to case_retention_extra (in addition to case_overflow)
-- v16 add retention_policy_type_heritage_mapping table
-- v17 replace definition of case_retention_extra
--     add pk to retention_policy_type_heritage_mapping
--     amend manual_retention_override and actual_case_closed_flag to int from bool
--v18  add default to created_ts on case_overflow
--     drop cas_id
--     add case_object_id and audio_folder_object_id
--     add new pk to case_overflow, relegate old pk to fk, and add field case_object_id

SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

--List of Table Aliases
-- case_management_retention              CMR
-- rps_retainer                           RPR
-- case_retention                         CAR  
-- retention_confidence_category_mapper   RCC
-- retention_policy_type                  RPT
-- case_retention_audit_heritage          RAH
-- retention_policy_type_heritage_mapping RHM


CREATE TABLE case_management_retention
(cmr_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,rpt_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       NOT NULL                
,total_sentence              CHARACTER VARYING                       -- < is this integer or the nYnMnD >
) TABLESPACE pg_default;

CREATE TABLE rps_retainer
(rpr_id                         INTEGER                       NOT NULL
,rpt_id                         INTEGER                       NOT NULL
,rps_retainer_object_id         CHARACTER VARYING             NOT NULL -- all data will be from legacy
,is_current                     BOOLEAN
,case_object_id                 CHARACTER VARYING
,audio_folder_object_id         CHARACTER VARYING
,dm_retainer_root_id            CHARACTER VARYING
,dm_retention_rule_type         INTEGER
,dm_retention_date              TIMESTAMP WITH TIME ZONE               -- retaining _date to indictate source
,dmc_current_phase_id           CHARACTER VARYING
,dmc_entry_date                 TIMESTAMP WITH TIME ZONE      
,dmc_parent_ancestor_id         CHARACTER VARYING                      -- most coincide with PK, but 1000s dont
,dmc_phase_name                 CHARACTER VARYING                      -- is active or final
,dmc_qualification_date         TIMESTAMP WITH TIME ZONE               -- retaining _date to indictate source       
,dmc_retention_base_date        TIMESTAMP WITH TIME ZONE               -- retaining _date to indictate source
,dmc_retention_policy_id        CHARACTER VARYING
,dmc_ultimate_ancestor_id       CHARACTER VARYING
,dmc_vdm_retention_rule         INTEGER
,dmc_is_superseded              INTEGER                                -- retaining integer instead to mapping to bool
,dmc_superseded_date            TIMESTAMP WITH TIME ZONE               -- retaining _date to indictate source
,dmc_superseded_phase_id        CHARACTER VARYING   
,dmc_snapshot_retention_rule    INTEGER
,dmc_approval_required          INTEGER
,dmc_approval_status            CHARACTER VARYING
,dmc_approved_date              TIMESTAMP WITH TIME ZONE
,dmc_projected_disposition_date TIMESTAMP WITH TIME ZONE
,dmc_is_qualification_suspended INTEGER 
,dmc_suspension_lift_date       TIMESTAMP WITH TIME ZONE
,dmc_base_date_override         TIMESTAMP WITH TIME ZONE
,dms_object_name                CHARACTER VARYING
,dms_i_chronicle_id             CHARACTER VARYING
,dms_r_policy_id                CHARACTER VARYING
,dms_r_resume_state             INTEGER
,dms_r_current_state            INTEGER
,created_ts                     TIMESTAMP WITH TIME ZONE      NOT NULL -- dms_r_creator_name
,created_by                     INTEGER                       NOT NULL -- dms_r_creation_date
,last_modified_ts               TIMESTAMP WITH TIME ZONE      NOT NULL -- dms_r_modifier
,last_modified_by               INTEGER                       NOT NULL -- dms_r_modify_date
) TABLESPACE pg_default;

COMMENT ON TABLE  rps_retainer
IS 'is essentially a legacy table, based on the component tables necessary to derive the dmc_rps_retainer object';
COMMENT ON COLUMN rps_retainer.rpr_id
IS 'primary key of case_rps_retainer';

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
(rah_id                          INTEGER                       NOT NULL -- synthetic PK, name to deconflict with courthouse_region_ae
,cas_id                          INTEGER                                -- ideally should be N/N
,rpt_id                          INTEGER                                -- ideally should be N/N
,case_retention_audit_object_id  CHARACTER VARYING(16)         NOT NULL -- can be N/N as direct from legacy
,is_current                      BOOLEAN
,i_partition                     INTEGER
,c_case_id                       CHARACTER VARYING(32)         
,c_date_retention_amended        TIMESTAMP WITH TIME ZONE
,c_comments                      CHARACTER VARYING
,c_date_previous_retention       TIMESTAMP WITH TIME ZONE
,c_username                      INTEGER
,c_status                        CHARACTER VARYING(32)
,c_courthouse                    CHARACTER VARYING(64)
,c_policy_type                   CHARACTER VARYING(20)
,c_case_closed_date              TIMESTAMP WITH TIME ZONE
,object_name                     CHARACTER VARYING(255)
,r_creator_name                  INTEGER
,r_creation_date                 TIMESTAMP WITH TIME ZONE
,r_modifier                      INTEGER
,r_modify_date                   TIMESTAMP WITH TIME ZONE
,owner_name                      INTEGER
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

CREATE TABLE case_overflow
(cof_id                      INTEGER NOT NULL
,cas_id                      INTEGER
,rpt_id                      INTEGER
,case_total_sentence         CHARACTER VARYING
,retention_event_ts          TIMESTAMP WITH TIME ZONE     
,case_retention_fixed        CHARACTER VARYING
,retention_applies_from_ts   TIMESTAMP WITH TIME ZONE
,end_of_sentence_date_ts     TIMESTAMP WITH TIME ZONE
,manual_retention_override   INTEGER
,retain_until_ts             TIMESTAMP WITH TIME ZONE
,c_closed_pre_live           INTEGER
,c_case_closed_date_pre_live TIMESTAMP WITH TIME ZONE
,case_created_ts             TIMESTAMP WITH TIME ZONE
,case_object_id              CHARACTER VARYING
,audio_folder_object_id      CHARACTER VARYING(16)
,case_last_modified_ts       TIMESTAMP WITH TIME ZONE                -- to support delta, when case changed
,audio_last_modified_ts      TIMESTAMP WITH TIME ZONE                -- to suppor delta, when moj_audio_folder changes
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL     DEFAULT current_timestamp
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
) TABLESPACE pg_default;

CREATE TABLE case_retention_extra
(cas_id                        INTEGER    NOT NULL
,current_rah_id                INTEGER
,current_rah_rpt_id            INTEGER
,current_rpr_id                INTEGER
,current_rpr_rpt_id            INTEGER
,retention_fixed_rpt_id        INTEGER
,case_total_sentence           CHARACTER VARYING
,case_retention_fixed          CHARACTER VARYING
,end_of_sentence_date_ts       TIMESTAMP WITH TIME ZONE
,manual_retention_override     INTEGER
,actual_case_closed_flag       INTEGER
,actual_case_closed_ts         TIMESTAMP WITH TIME ZONE
,actual_retain_until_ts        TIMESTAMP WITH TIME ZONE
,actual_case_created_ts        TIMESTAMP WITH TIME ZONE
,submitted_by                  INTEGER
,rps_retainer_object_id        CHARACTER VARYING
,case_closed_eve_id            INTEGER
,case_closed_event_ts          TIMESTAMP WITH TIME ZONE
,max_event_ts                  TIMESTAMP WITH TIME ZONE
,max_media_ts                  TIMESTAMP WITH TIME ZONE
,closure_method_type           CHARACTER VARYING
,best_case_closed_ts           TIMESTAMP WITH TIME ZONE
,best_case_closed_type         CHARACTER VARYING
,best_retainer_retain_until_ts TIMESTAMP WITH TIME ZONE
,best_audit_retain_until_ts    TIMESTAMP WITH TIME ZONE
,retention_aged_policy_name     CHARACTER VARYING
,case_closed_diff_in_days      INTEGER
,r_retain_until_diff_in_days   INTEGER
,a_retain_until_diff_in_days   INTEGER
,validation_error_1            CHARACTER VARYING
,validation_error_2            CHARACTER VARYING
,validation_error_3            CHARACTER VARYING
,validation_error_4            CHARACTER VARYING
,validation_error_5            CHARACTER VARYING
,ret_conf_score                INTEGER
,ret_conf_reason               CHARACTER VARYING
,ret_conf_updated_ts           TIMESTAMP WITH TIME ZONE
,validated_ts                  TIMESTAMP WITH TIME ZONE
,created_ts                    TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_ts              TIMESTAMP WITH TIME ZONE      NOT NULL
,migrated_ts                    TIMESTAMP WITH TIME ZONE
) TABLESPACE pg_default;

CREATE TABLE  retention_policy_type_heritage_mapping
(rhm_id                        INTEGER                       NOT NULL
,heritage_policy_name          CHARACTER VARYING             NOT NULL
,heritage_table                CHARACTER VARYING             NOT NULL
,modernised_rpt_id             INTEGER                       NOT NULL
) TABLESPACE pg_default;


CREATE UNIQUE INDEX case_management_retention_pk ON case_management_retention(cmr_id) TABLESPACE pg_default;
ALTER TABLE case_management_retention ADD PRIMARY KEY USING INDEX case_management_retention_pk;

CREATE UNIQUE INDEX case_retention_pk ON case_retention(car_id) TABLESPACE pg_default; 
ALTER TABLE case_retention  ADD PRIMARY KEY USING INDEX case_retention_pk;

CREATE UNIQUE INDEX retention_confidence_category_mapper_pk ON retention_confidence_category_mapper(rcc_id) TABLESPACE pg_default; 
ALTER TABLE retention_confidence_category_mapper ADD PRIMARY KEY USING INDEX retention_confidence_category_mapper_pk;

CREATE UNIQUE INDEX retention_policy_type_pk ON retention_policy_type(rpt_id) TABLESPACE pg_default; 
ALTER TABLE retention_policy_type ADD PRIMARY KEY USING INDEX retention_policy_type_pk;

CREATE UNIQUE INDEX rps_retainer_pk ON rps_retainer(rpr_id) TABLESPACE pg_default; 
ALTER TABLE rps_retainer ADD PRIMARY KEY USING INDEX rps_retainer_pk;

CREATE UNIQUE INDEX case_overflow_pk ON case_overflow(cof_id) TABLESPACE pg_default;
ALTER TABLE case_overflow ADD PRIMARY KEY USING INDEX case_overflow_pk;

CREATE UNIQUE INDEX case_retention_extra_pk ON case_retention_extra(cas_id) TABLESPACE pg_default; 
ALTER TABLE case_retention_extra ADD PRIMARY KEY USING INDEX case_retention_extra_pk;


CREATE UNIQUE INDEX case_retention_audit_heritage_pk ON case_retention_audit_heritage(rah_id) TABLESPACE pg_default; 
ALTER TABLE case_retention_audit_heritage ADD PRIMARY KEY USING INDEX case_retention_audit_heritage_pk;

CREATE UNIQUE INDEX retention_policy_type_heritage_mapping_pk ON retention_policy_type_heritage_mapping(rhm_id) TABLESPACE pg_default; 
ALTER TABLE retention_policy_type_heritage_mapping ADD PRIMARY KEY USING INDEX retention_policy_type_heritage_mapping_pk;

CREATE SEQUENCE cmr_seq CACHE 20;
CREATE SEQUENCE rpr_seq CACHE 20;
CREATE SEQUENCE car_seq CACHE 20;
CREATE SEQUENCE rcc_seq CACHE 20;
CREATE SEQUENCE rpt_seq CACHE 20;
CREATE SEQUENCE rah_seq CACHE 20;
CREATE SEQUENCE rhm_seq CACHE 20;
CREATE SEQUENCE cof_seq CACHE 20;

ALTER TABLE rps_retainer
ADD CONSTRAINT rps_retainer_retention_policy_type_fk
FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE rps_retainer      
ADD CONSTRAINT rps_retainer_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE rps_retainer      
ADD CONSTRAINT rps_retainer_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_audit_heritage       
ADD CONSTRAINT case_retention_audit_heritage_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_retention_audit_heritage       
ADD CONSTRAINT case_retention_audit_heritage_retention_policy_type_fk
FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE case_retention_audit_heritage       
ADD CONSTRAINT case_retention_audit_heritage_c_username_fk
FOREIGN KEY (c_username) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_audit_heritage       
ADD CONSTRAINT case_retention_audit_heritage_r_creator_name_fk
FOREIGN KEY (r_creator_name) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_audit_heritage       
ADD CONSTRAINT case_retention_audit_heritage_r_modifier_fk
FOREIGN KEY (r_modifier) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_audit_heritage       
ADD CONSTRAINT case_retention_audit_heritage_owner_name_fk
FOREIGN KEY (owner_name) REFERENCES user_account(usr_id);

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

ALTER TABLE case_overflow                      
ADD CONSTRAINT case_overflow_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_overflow                      
ADD CONSTRAINT case_overflow_retention_policy_type_fk
FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE case_retention_extra                     
ADD CONSTRAINT case_retention_extra_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);


GRANT SELECT,INSERT,UPDATE,DELETE ON case_management_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON rps_retainer TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_confidence_category_mapper TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_policy_type TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_overflow TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_retention_extra TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_policy_type_heritage_mapping TO darts_user;

GRANT SELECT,UPDATE ON  cmr_seq TO darts_user;
GRANT SELECT,UPDATE ON  rpr_seq TO darts_user;
GRANT SELECT,UPDATE ON  car_seq TO darts_user;
GRANT SELECT,UPDATE ON  rcc_seq TO darts_user;
GRANT SELECT,UPDATE ON  rpt_seq TO darts_user;
GRANT SELECT,UPDATE ON  rah_seq TO darts_user;
GRANT SELECT,UPDATE ON  rhm_seq TO darts_user;
GRANT SELECT,UPDATE ON cof_seq TO darts_user;
