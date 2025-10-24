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
--v19  remove cas_id from rps_retainer and corresponding foreign key
--     add case_object_id, audio_folder_object_id to rps_retainer
--     add cof_id to case_overflow
--     change PK on case_overflow from cas_id to cof_id
--     add sequence cof_seq 
--     add case_object_id to case_overflow
--v20  remove not null from cas_id on case_overflow
--     add c_case_id, c_courthouse, c_type, c_upload_priority, c_reporting_restrictions,
--     case_object_name, r_folder_path, c_interpreter_used to case_overflow
--v21  remove caching from sequences
--v22  adding 4 tables ( cc_dets, cmr_dets, cr_dets, wk_case_correction ) to support retention preparation
--v23  adding 3 tables ( wk_case_best_values_p1, wk_case_best_values_post_p1,wk_case_activity_data) to support validation
--v24  adding 1 table, wk_case_confidence_level
--v25  amend cr_dets.retention_object_id to char(16)
--     adding wk_crah_is_current_by_creation, wk_rr_is_current_by_creation
--     adding wk_crah_is_current_by_logic,    wk_rr_is_current_by_logic
--     adding single column to wk_case_correction and wk_case_activity_data
--v26  remove rownum column from wk_case_activity_data
--v27  remove table case_retention_extra
--v28  amending a number of INT to BIGINT
--          eve_id(case_management_retention)
--v29  adding cmr_id to cmr_dets
--v30  add car_id to wk_cr_dets_aligned ( whilst also adding the table !)


SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

--List of Table Aliases
-- case_retention                         CAR  
-- case_management_retention              CMR
-- case_retention_audit_heritage          RAH
-- retention_confidence_category_mapper   RCC
-- retention_policy_type_heritage_mapping RHM
-- rps_retainer                           RPR
-- retention_policy_type                  RPT

-- cc_dets                                CCD
-- cmr_dets                               CMD
-- cr_dets                                CRD
-- wk_case_correction                     WCC

--wk_case_best_values_p1                  WBV
--wk_case_best_values_post_p1             WBP
--wk_case_activity_data                   WCA
--wk_case_confidence_level                WCL

--wk_crah_is_current_by_creation          CBC
--wk_crah_is_current_by_logic             CBL
--wk_rr_is_current_by_creation            RBC
--wk_rr_is_current_by_logic               RBL

--wk_cr_dets_aligned                      WCDA



CREATE TABLE case_management_retention
(cmr_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,rpt_id                      INTEGER                       NOT NULL
,eve_id                      BIGINT                        NOT NULL                
,total_sentence              CHARACTER VARYING                       -- < is this integer or the nYnMnD >
) TABLESPACE pg_default;

CREATE TABLE rps_retainer
(rpr_id                         INTEGER                       NOT NULL                  
,rpt_id                         INTEGER                       NOT NULL
,rps_retainer_object_id         CHARACTER VARYING             NOT NULL -- all data will be from legacy
,case_object_id                 CHARACTER VARYING
,audio_folder_object_id         CHARACTER VARYING
,is_current                     BOOLEAN 
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
(cof_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       
,rpt_id                      INTEGER
,case_object_id              CHARACTER VARYING
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
,audio_folder_object_id      CHARACTER VARYING(16)
,case_last_modified_ts       TIMESTAMP WITH TIME ZONE                -- to support delta, when case changed
,audio_last_modified_ts      TIMESTAMP WITH TIME ZONE                -- to suppor delta, when moj_audio_folder changes
,c_case_id                   CHARACTER VARYING(32)
,c_courthouse                CHARACTER VARYING(64)
,c_type                      CHARACTER VARYING(32)                   -- exclusively 1 or null
,c_upload_priority           INTEGER                                 -- number(10) in legacy, but contains only 0 !
,c_reporting_restrictions    CHARACTER VARYING(128)
,case_object_name            CHARACTER VARYING(255)                  -- from dm_sysobject_s.object_name
,r_folder_path               CHARACTER VARYING(740)                  -- from dm_folder_r.r_folder_path
,c_interpreter_used          INTEGER                                 -- number(1) in legacy, contains 0/1/null, hence is really bool
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL     DEFAULT current_timestamp
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
) TABLESPACE pg_default;


CREATE TABLE  retention_policy_type_heritage_mapping
(rhm_id                        INTEGER                       NOT NULL
,heritage_policy_name          CHARACTER VARYING             NOT NULL
,heritage_table                CHARACTER VARYING             NOT NULL
,modernised_rpt_id             INTEGER                       NOT NULL
) TABLESPACE pg_default;

CREATE TABLE cc_dets
(cas_id                        INTEGER
,case_object_id                CHARACTER VARYING(16)
,c_courthouse                  CHARACTER VARYING(64)
,c_case_id                     CHARACTER VARYING(32)
,c_closed_pre_live             INTEGER
,c_case_closed_date_pre_live   TIMESTAMP WITH TIME ZONE
,c_case_closed_date_crah       TIMESTAMP WITH TIME ZONE
,case_created_ts               TIMESTAMP WITH TIME ZONE
,cas_retention_fixed           CHARACTER VARYING(16)
,case_total_sentence           CHARACTER VARYING(16)
,retention_applies_from_ts     TIMESTAMP WITH TIME ZONE
,end_of_sentence_date_ts       TIMESTAMP WITH TIME ZONE
,manual_retention_override     INTEGER
,retain_until_ts               TIMESTAMP WITH TIME ZONE
,audio_folder_object_id        CHARACTER VARYING(16)
,case_closed_event_event_ts_by_pre_live_closed_date           TIMESTAMP WITH TIME ZONE
,case_closed_event_event_ts_eve_id_by_pre_live_closed_date    INTEGER
,case_closed_event_created_ts_by_pre_live_closed_date         TIMESTAMP WITH TIME ZONE
,case_closed_event_created_ts_eve_id_by_pre_live_closed_date  INTEGER
,case_closed_event_event_ts_by_audit_closed_date              TIMESTAMP WITH TIME ZONE
,case_closed_event_event_ts_eve_id_by_audit_closed_date       INTEGER
,case_closed_event_created_ts_by_audit_closed_date            TIMESTAMP WITH TIME ZONE
,case_closed_event_created_ts_eve_id_by_audit_closed_date     INTEGER
,case_closed_event_event_ts_case_closed_event_latest          TIMESTAMP WITH TIME ZONE
,case_closed_event_event_ts_eve_id_case_closed_event_latest   INTEGER
,case_closed_event_created_ts_case_closed_event_latest        TIMESTAMP WITH TIME ZONE
,case_closed_event_created_ts_eve_id_case_closed_event_latest INTEGER
,latest_event_by_event_ts_ts                                  TIMESTAMP WITH TIME ZONE
,latest_event_by_event_ts_eve_id                              INTEGER
,latest_event_by_created_ts                                   TIMESTAMP WITH TIME ZONE
,latest_event_by_created_ts_eve_id                            INTEGER
,latest_hearing_ended_event_by_event_ts                       TIMESTAMP WITH TIME ZONE
,latest_hearing_ended_event_by_event_ts_eve_id                INTEGER
,latest_hearing_ended_event_by_created_ts                     TIMESTAMP WITH TIME ZONE
,latest_hearing_ended_event_by_created_ts_eve_id              INTEGER
,latest_log_event_by_event_ts                                 TIMESTAMP WITH TIME ZONE
,latest_log_event_by_event_ts_eve_id                          INTEGER
,latest_log_event_by_created_ts                               TIMESTAMP WITH TIME ZONE
,latest_log_event_by_created_ts_eve_id                        INTEGER
,latest_sentencing_event_by_event_ts                          TIMESTAMP WITH TIME ZONE
,latest_sentencing_event_by_event_ts_eve_id                   INTEGER
,latest_sentencing_event_by_created_ts                        TIMESTAMP WITH TIME ZONE
,latest_sentencing_event_by_created_ts_eve_id                 INTEGER
,latest_sentencing_271_event_by_event_ts                      TIMESTAMP WITH TIME ZONE
,latest_sentencing_271_event_by_event_ts_eve_id               INTEGER
,latest_sentencing_271_event_by_created_ts                    TIMESTAMP WITH TIME ZONE
,latest_sentencing_271_event_by_created_ts_eve_id             INTEGER
,latest_media_by_created_ts                                   TIMESTAMP WITH TIME ZONE
,latest_media_by_created_ts_eve_id                            INTEGER
,latest_media_by_start_ts                                     TIMESTAMP WITH TIME ZONE
,latest_media_by_start_ts_eve_id                              INTEGER
,latest_media_by_end_ts                                       TIMESTAMP WITH TIME ZONE
,latest_media_by_end_ts_eve_id                                INTEGER
,latest_warrant_event_by_event_ts                             TIMESTAMP WITH TIME ZONE
,latest_warrant_event_by_event_ts_eve_id                      INTEGER
,latest_warrant_event_by_created_ts                           TIMESTAMP WITH TIME ZONE
,latest_warrant_event_by_created_ts_eve_id                    INTEGER
,ret_conf_score                                               INTEGER
,ret_conf_reason                                              CHARACTER VARYING(64)
,ret_conf_updated_ts                                          TIMESTAMP WITH TIME ZONE
,category_type                                                CHARACTER VARYING(32)
,latest_activity_period                                       CHARACTER VARYING(5)
) TABLESPACE pg_default;

CREATE TABLE cmr_dets
(cmd_id                        SERIAL
,cmr_id                        INTEGER
,cas_id                        INTEGER
,rpt_id                        INTEGER
,eve_id                        BIGINT
,total_sentence                CHARACTER VARYING(32)
) TABLESPACE pg_default;

CREATE TABLE cr_dets
(crd_id                        SERIAL
,cas_id                        INTEGER
,rpt_id                        INTEGER
,cmd_id                        INTEGER
,total_sentence                CHARACTER VARYING(32)
,retain_until_ts               TIMESTAMP WITH TIME ZONE
,retain_until_applied_on_ts    TIMESTAMP WITH TIME ZONE
,current_state                 CHARACTER VARYING(32)
,comments                      CHARACTER VARYING(150)
,confidence_category           INTEGER
,retention_object_id           CHARACTER VARYING(16)   
,submitted_by                  INTEGER
,created_ts                    TIMESTAMP WITH TIME ZONE
,created_by                    INTEGER         
,last_modified_ts              TIMESTAMP WITH TIME ZONE    
,last_modified_by              INTEGER             
) TABLESPACE pg_default;

CREATE TABLE wk_case_correction
(cas_id                        INTEGER
,category_type                 CHARACTER VARYING(32)
,case_closed_date_corr         BOOLEAN
,case_closed_corr              BOOLEAN
,retain_until_ts_corr          BOOLEAN
,new_case_closed               BOOLEAN
,new_closed_date_ts            TIMESTAMP WITH TIME ZONE
,case_old_closed_date_ts       TIMESTAMP WITH TIME ZONE
,case_audit_old_closed_date_ts TIMESTAMP WITH TIME ZONE
,closed_date_type              CHARACTER VARYING(50)
,eve_id                        BIGINT
,cmr_eve_id                    BIGINT
,current_logic_rpt_id          INTEGER
,current_creation_rpt_id       INTEGER
,case_total_sentence           CHARACTER VARYING(16)
,new_retain_until_ts           TIMESTAMP WITH TIME ZONE
,retain_until_applied_on_ts    TIMESTAMP WITH TIME ZONE
,current_state                 CHARACTER VARYING(16)
,old_state                     CHARACTER VARYING(16)
,retention_object_id           CHARACTER VARYING(16)
,latest_activity_period        CHARACTER VARYING(5)
,best_closed_date_period       CHARACTER VARYING(5)
,adjusted_closed_date_ts       TIMESTAMP WITH TIME ZONE
,adjusted_closed_date_type     CHARACTER VARYING(50)
,adjusted_eve_id               BIGINT
,has_warrant                   BOOLEAN
,warrant_before_sentencing     BOOLEAN
,case_closed_before_sentencing BOOLEAN
) TABLESPACE pg_default;

CREATE TABLE wk_case_best_values_p1
(cas_id                         INTEGER
,eve_id                         BIGINT
,evh_id                         INTEGER
,closed_date_ts                 TIMESTAMP WITH TIME ZONE
,closed_date_type               CHARACTER VARYING(50)
,rownum                         INTEGER
) TABLESPACE pg_default;

CREATE TABLE wk_case_best_values_post_p1
(cas_id                         INTEGER
,eve_id                         BIGINT
,evh_id                         INTEGER
,closed_date_ts                 TIMESTAMP WITH TIME ZONE
,closed_date_type               CHARACTER VARYING(50)
,rownum                         INTEGER
) TABLESPACE pg_default;

CREATE TABLE wk_case_activity_data
(cas_id                         INTEGER                    NOT NULL
,eve_id                         BIGINT
,evh_id                         INTEGER
,closed_date_ts                 TIMESTAMP WITH TIME ZONE
,closed_date_type               CHARACTER VARYING(100)
) TABLESPACE pg_default;

CREATE TABLE wk_case_confidence_level
(cas_id                         INTEGER                    NOT NULL
,confidence_level               INTEGER
) TABLESPACE pg_default;

CREATE TABLE wk_crah_is_current_by_creation
(cas_id                         INTEGER
,rah_id                         INTEGER
,rpt_id                         INTEGER
,c_policy_type                  CHARACTER VARYING(20)
,c_courthouse                   CHARACTER VARYING(64)
,c_case_id                      CHARACTER VARYING(32)
,r_creation_date                TIMESTAMP WITH TIME ZONE
,r_modify_date                  TIMESTAMP WITH TIME ZONE
,c_status                       CHARACTER VARYING(32)
,c_username                     INTEGER
,case_retention_audit_object_id CHARACTER VARYING(16)
,ready_retain_until_date        TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
) TABLESPACE pg_default;

CREATE TABLE wk_crah_is_current_by_logic
(cas_id                         INTEGER
,rah_id                         INTEGER
,rpt_id                         INTEGER
,c_policy_type                  CHARACTER VARYING(20)
,c_courthouse                   CHARACTER VARYING(64)
,c_case_id                      CHARACTER VARYING(32)
,r_creation_date                TIMESTAMP WITH TIME ZONE
,c_status                       CHARACTER VARYING(32)
,c_username                     INTEGER
,case_retention_audit_object_id CHARACTER VARYING(16)
,ready_retain_until_date        TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
) TABLESPACE pg_default;

CREATE TABLE wk_rr_is_current_by_creation
(cas_id                         INTEGER
,rpr_id                         INTEGER
,rpt_id                         INTEGER
,case_object_id                 CHARACTER VARYING(16)
,dm_retainer_root_id            CHARACTER VARYING(16)
,dms_object_name                CHARACTER VARYING(64)
,created_ts                     TIMESTAMP WITH TIME ZONE
,last_modified_ts               TIMESTAMP WITH TIME ZONE
,rps_retainer_object_id         CHARACTER VARYING(16)
,dm_retention_date              TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
) TABLESPACE pg_default;

CREATE TABLE wk_rr_is_current_by_logic
(cas_id                         INTEGER
,rpr_id                         INTEGER
,rpt_id                         INTEGER
,case_object_id                 CHARACTER VARYING(16)
,dm_retainer_root_id            CHARACTER VARYING(16)
,dms_object_name                CHARACTER VARYING(64)
,created_ts                     TIMESTAMP WITH TIME ZONE
,rps_retainer_object_id         CHARACTER VARYING(16)
,dm_retention_date              TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
) TABLESPACE pg_default;

CREATE TABLE wk_cr_dets_aligned
(wcda_id                        INTEGER
,car_id                         INTEGER
,cas_id                         INTEGER
,rpt_id                         INTEGER
,cmd_id                         INTEGER
,total_sentence                 CHARACTER VARYING(32)
,retain_until_ts                TIMESTAMP WITH TIME ZONE
,retain_until_applied_on_ts     TIMESTAMP WITH TIME ZONE
,current_state                  CHARACTER VARYING(32)
,comments                       CHARACTER VARYING(150)
,confidence_category            INTEGER
,retention_object_id            CHARACTER VARYING(16)
,submitted_by                   INTEGER
,created_ts                     TIMESTAMP WITH TIME ZONE
,created_by                     INTEGER
,last_modified_ts               TIMESTAMP WITH TIME ZONE
,last_modified_by               INTEGER
) TABLESPACE pg_default;

CREATE TABLE retention_process_log
(cas_id                         INTEGER
,cr_row_count                   INTEGER
,cmr_row_count                  INTEGER
,processed_ts                   TIMESTAMP WITH TIME ZONE
,status                         CHARACTER VARYING(10)
,message                        CHARACTER VARYING
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


CREATE UNIQUE INDEX case_retention_audit_heritage_pk ON case_retention_audit_heritage(rah_id) TABLESPACE pg_default; 
ALTER TABLE case_retention_audit_heritage ADD PRIMARY KEY USING INDEX case_retention_audit_heritage_pk;

CREATE UNIQUE INDEX retention_policy_type_heritage_mapping_pk ON retention_policy_type_heritage_mapping(rhm_id) TABLESPACE pg_default; 
ALTER TABLE retention_policy_type_heritage_mapping ADD PRIMARY KEY USING INDEX retention_policy_type_heritage_mapping_pk;

CREATE UNIQUE INDEX cc_dets_pk ON cc_dets(cas_id) TABLESPACE pg_default;
ALTER TABLE cc_dets ADD PRIMARY KEY USING INDEX cc_dets_pk;

CREATE UNIQUE INDEX cmr_dets_pk ON cmr_dets(cmd_id) TABLESPACE pg_default;
ALTER TABLE cmr_dets ADD PRIMARY KEY USING INDEX cmr_dets_pk;

CREATE UNIQUE INDEX cr_dets_pk ON cr_dets(crd_id) TABLESPACE pg_default;
ALTER TABLE cr_dets ADD PRIMARY KEY USING INDEX cr_dets_pk;

CREATE UNIQUE INDEX wk_case_correction_pk ON wk_case_correction(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_case_correction ADD PRIMARY KEY USING INDEX wk_case_correction_pk;

CREATE UNIQUE INDEX wk_case_best_values_p1_pk ON wk_case_best_values_p1(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_case_best_values_p1 ADD PRIMARY KEY USING INDEX wk_case_best_values_p1_pk;

CREATE UNIQUE INDEX wk_case_best_values_post_p1_pk ON wk_case_best_values_post_p1(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_case_best_values_post_p1 ADD PRIMARY KEY USING INDEX wk_case_best_values_post_p1_pk;

CREATE UNIQUE INDEX wk_case_activity_data_pk ON wk_case_activity_data(cas_id,closed_date_type) TABLESPACE pg_default;
ALTER TABLE wk_case_activity_data ADD PRIMARY KEY USING INDEX wk_case_activity_data_pk;

CREATE UNIQUE INDEX wk_case_confidence_level_pk ON wk_case_confidence_level(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_case_confidence_level ADD PRIMARY KEY USING INDEX wk_case_confidence_level_pk;

CREATE UNIQUE INDEX wk_crah_is_current_by_creation_pk ON wk_crah_is_current_by_creation(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_crah_is_current_by_creation ADD PRIMARY KEY USING INDEX wk_crah_is_current_by_creation_pk;

CREATE UNIQUE INDEX wk_crah_is_current_by_logic_pk ON wk_crah_is_current_by_logic(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_crah_is_current_by_logic ADD PRIMARY KEY USING INDEX wk_crah_is_current_by_logic_pk;

CREATE UNIQUE INDEX wk_rr_is_current_by_creation_pk ON wk_rr_is_current_by_creation(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_rr_is_current_by_creation ADD PRIMARY KEY USING INDEX wk_rr_is_current_by_creation_pk;

CREATE UNIQUE INDEX wk_rr_is_current_by_logic_pk ON wk_rr_is_current_by_logic(cas_id) TABLESPACE pg_default;
ALTER TABLE wk_rr_is_current_by_logic ADD PRIMARY KEY USING INDEX wk_rr_is_current_by_logic_pk;

CREATE UNIQUE INDEX wk_cr_dets_aligned_pk ON wk_cr_dets_aligned(wcda_id) TABLESPACE pg_default;
ALTER TABLE wk_cr_dets_aligned ADD PRIMARY KEY USING INDEX wk_cr_dets_aligned_pk;

CREATE UNIQUE INDEX retention_process_log_pk ON retention_process_log(cas_id) TABLESPACE pg_default;
ALTER TABLE retention_process_log ADD PRIMARY KEY USING INDEX retention_process_log_pk;

CREATE SEQUENCE cmr_seq CACHE 1;
CREATE SEQUENCE rpr_seq CACHE 1;
CREATE SEQUENCE car_seq CACHE 1;
CREATE SEQUENCE rcc_seq CACHE 1;
CREATE SEQUENCE rpt_seq CACHE 1;
CREATE SEQUENCE rah_seq CACHE 1;
CREATE SEQUENCE rhm_seq CACHE 1;
CREATE SEQUENCE cof_seq CACHE 1;


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



GRANT SELECT,INSERT,UPDATE,DELETE ON case_management_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON rps_retainer TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_confidence_category_mapper TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_policy_type TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_overflow TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_policy_type_heritage_mapping TO darts_user;

GRANT SELECT,UPDATE ON  cmr_seq TO darts_user;
GRANT SELECT,UPDATE ON  rpr_seq TO darts_user;
GRANT SELECT,UPDATE ON  car_seq TO darts_user;
GRANT SELECT,UPDATE ON  rcc_seq TO darts_user;
GRANT SELECT,UPDATE ON  rpt_seq TO darts_user;
GRANT SELECT,UPDATE ON  rah_seq TO darts_user;
GRANT SELECT,UPDATE ON  rhm_seq TO darts_user;
GRANT SELECT,UPDATE ON  cof_seq TO darts_user;
