--v6 add sequences, remove character/numeric size limits, change DATE to TIMESTAMP
--v7 consistently add the legacy primary and foreign keys
--v8 3NF courthouses
--v9 remove various legacy columns
--v10 change some numeric columns to boolean, remove unused legacy column c_upload_priority
--v11 introduce many:many case:hearing, removed version label & superceded from moj_hearing, as no source for migration, and assume unneeded by modernised
--v12 remove reporting_restrictions from annotation, cached_media, event, media, transcription, transformation_request
--    add message_id, event_type_id to moj_event
--    add moj_event_type table and links to moj_event by FK
--v13 adding Not null to moj_transcription FK moj_cas_id & moj_crt_id, 
--    adding moj_transcription_type table to replace c_type. Remove fields c_notification_type, c_urgent from transcription
--    add event_name to moj_event
--    add moj_urgency table and fk to moj_transcription
--    added comment_ts and author to moj_transcription_comment
--v14 removing unneeded columns from moj_courthouse, normalising crown court code from daily list 
--    amending judge, defendant, defence, prosecutor on hearing to be 1-d array instead of scalar
--    rename i_version_label to i_version
--v15 remove moj_crt_id from case and corresponding FK
--v16 add moj_hea_id to transcription and corresponding FK
--    add moj_user to this script
--v17 further comments regarding properties of live data
--v18 moving attributes from moj_hearing to moj_case, changing timestamps to ts with tz
--v19 amended courthouse_name to be unique, amended courthouse_code to be integer
--    removing c_scheduled_start from moj_case, to be replaced by 2 columns on moj_hearing, scheduled_start_time and hearing_is_actual flag
--v20 moving moj_event and moj_media to link to moj_hearing rather than moj_case, resulting in moj_case_event_ae and 
--    moj_case_media_ae changing name
--v21 normalising c_reporting_restrictions into moj_reporting_restriction table
--    change alias for courthouse from CRT to CTH, accommodate new COURTROOM table aliased to CTR
--    add COURTROOM table, replace existing FKs to COURTHOUSE with ones to COURTROOM for event, media
--    rename moj_event_type.type to .evt_type 
--    remove c_courtroom from moj_annotation, moj_cached_media, moj_event, moj_hearing, moj_media, 
--    moj_transcription, moj_transformation_request
--    Remove associative entity case_hearing, replace with simple PK-FK relation
--v22 updated all sequences to cache 20
--    updated moj_daily_list and introduced moj_notification
--v23 remove moj_transformation_request, moj_transformation_log, moj_cached_media, replace with moj_media_request
--v24 adding request_type to moj_media_request omitted in error
--    amending external_object_directory to store one external address per record
--    amended c_case_id to c_case_number
--    replacing all smallint with integer, which includes all use of i_version
--    moj_courthouse.courthouse_code, moj_media_request.req_proc_attempts, moj_notification.send_attempts
--    remove i_version, r_version_label and i_version_label from moj_case, due to no legacy versioned data
--    amend daily_list content column to be character varying from xml, to store list in JSON format
--    remove c_type from moj_case
--v25 adding tablespace clauses to tables and indexes
--v26 adding multi-column unique constraints to moj_courtroom and moj_hearing
--v27 adding c_case_id to moj_event, adding transfer_attempts to both object_directory Tables
--    changing checksum from uuid to character varying
--v28 removing all moj_ prefixes to table and pk columns
--    reinstating moj_cth_id to case table
--v29 removing c_, r_, i_ prefixes to column names, switching daily_list content back to character varying
--    reinstating FK from case to courthouse
--    amending start and end on daily_list to be DATE, no time component
--    adding suffix of _list where [] is used on datatype to denote an array
--    rename event_type to event_handler
--    added external_location_type table
--v30 added standing data for reporting restrictions
--    added region table and associative entity to courthouse
--    added device_register table (equivalent to legacy tbl_moj_node)
--    added unique constraint on court_case(cth_id, case_number)
--    standardised the use of "last_modified_ts" , where previously using "modified_ts" or "last_updated_ts"
--    standardised the use of "last_modified_by" , where previously using "modified_by"
--    reduced number of Documentum columns on user_account table, while adding a few others
--v31 add darts_owner and darts_user accounts and amend security accordingly
--v32 introduce defendant, prosecutor, defence tables to remove the need for character varying arrays on court_case, and add foreign keys to court_case
--    introduce judge table to remove the need for character varying array on hearing, and add foreign key to hearing
--    correct name of reporting_restriction_pk and case of the table name from pleural to singular
--    add not null constraint to PK columns on region and user_account (should be inferable, but hibernate likes it explicitly defined)
--    amend NUMERIC to INTEGER on user_account and event tables
--v33 remove synthetic PK from associative entities hearing_events_ae and hearing_media_as, replace with PK on natural key
--v34 add case_retention, retention_policy & case_retention_event tables
--v35 remove reporting_restrictions table, replace with foreign key on case to event_handler and add boolean to event_handler
--v36 amend defence to defendant on defendant_name table
--    remove foreign key on transcription table to hearing
--v37 reinstate hea_id on transcription and FK to hearing
--    add courthouse fk on transcription 
--    add originating_courtroom on court_case
--    amend names of judge, prosecutor, defence, defendant  
--    add associative entity hearing_judge_ae and amend FKs accordingly
--v38 add cas_id to judge table and foreign key
--    add unique constraint on jud.judge_name
--v39 remove origating_courtroom from court_case
--v40 changed judge table to contain only (jud_id integer PK, judge_name character varying UK)
--    created table case_judge_ae to contain (cas_id, jud_id) composite PK    
--v41 add table automated_task
--v42 add tables audit, audit_activity, external_service_auth_token and associated constraints and sequences
--    rename table urgency to transcription_urgency
--    add new additional check constraints section
--v43 removed superseded and version from schema
--    added following columns to all tables (apart from ae tables - not supported by hibernate) and FKs to user_account
--      ,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
--      ,created_by                  INTEGER                       NOT NULL
--      ,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
--      ,last_modified_by            INTEGER                       NOT NULL  
--    updated daily-list - rename daily_list_content to daily_list_content_json and add new column daily_list_content_xml
--    moved INSERT statements to new file standing_data.sql
--v44 removed PK device_register.der_id 
--    added new PK device_register.node_id
--    added new column device_register.device_type 
--    added new set defaults section 
--    added default value DAR to column device_register.device_type
--v45 add expiry_ts to media_request, amend request_status, request_type, start_ts, end_ts to not null
--    rename media_hearing_fk to media_request_hearing_fk as per naming convention
--    add foreign key on media_request.requestor to user_account
--v46 add transcription_workflow table, to remove workflow related elements from transcription.  
--    adding record_id and file_id to external_object_directory to allow full addressability for items needing to fields
--    add current_owner to media_request, to allow requestor and current_owner to diverge following creation
--    add is_log_entry to event, to differentiate between events coming from case management / mid tier
--    added automated_task.task_enabled as a not null boolean
--v47 adding NN to hostname, ip_address, mac_address on device_register
--    amending der_seq to restart at 50000, above highest legacy value of 30121
--    removed PK courthouse_region_ae.cra_id and associated sequence
--    added composite PK courthouse_region_ae (cth_id,reg_id)
--    added table transcription_status
--    removed column transcription.current_state
--    added column transcription.trs_id and FK to transcription_status

-- List of Table Aliases
-- annotation                  ANN
-- audit                       AUD
-- audit_activity              AUA
-- automated_task              AUT
-- case_judge_ae               CAJ
-- case_retention              CAR
-- case_retention_event        CRE
-- court_case                  CAS
-- courthouse                  CTH
-- courthouse_region_ae        CRA
-- courtroom                   CTR
-- daily_list                  DAL
-- defence                     DFC
-- defendant                   DFD
-- device_register             DER
-- event                       EVE
-- event_handler               EVH
-- external_object_directory   EOD
-- external_service_auth_token ESA
-- hearing                     HEA
-- hearing_event_ae            HEE
-- hearing_media_ae            HEM
-- hearing_judge_ae            HEJ
-- judge                       JUD
-- media                       MED
-- media_request               MER
-- notification                NOT
-- object_directory_status     ODS
-- prosecutor                  PRN
-- region                      REG
-- report                      REP
-- retention_policy            RTP
-- transcription               TRA
-- transcription_comment       TRC
-- transcription_status        TRS
-- transcription_type          TRT
-- transcription_urgency       TRU
-- transcription_workflow      TRW
-- transient_object_directory  TOD
-- user_account                USR

CREATE USER darts_owner with
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION
PASSWORD 'darts_owner';

CREATE USER darts_user with
NOSUPERUSER
NOINHERIT
NOCREATEDB
NOCREATEROLE
NOREPLICATION
PASSWORD 'darts_user';

CREATE SCHEMA DARTS AUTHORIZATION DARTS_OWNER;

CREATE TABLESPACE darts_tables  location 'E:/PostgreSQL/Tables';
CREATE TABLESPACE darts_indexes location 'E:/PostgreSQL/Indexes';

GRANT ALL ON TABLESPACE darts_tables TO darts_owner;
GRANT ALL ON TABLESPACE darts_indexes TO darts_owner;

SET ROLE DARTS_OWNER;

SET SEARCH_PATH TO darts;

CREATE TABLE annotation
(ann_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER
,annotation_text             CHARACTER VARYING
,annotation_ts               TIMESTAMP WITH TIME ZONE
,annotation_object_id        CHARACTER VARYING(16)
,version_label               CHARACTER VARYING(32)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL          
) TABLESPACE darts_tables;

COMMENT ON COLUMN annotation.ann_id
IS 'primary key of annotation';

COMMENT ON COLUMN annotation.cas_id
IS 'foreign key from court_case';

COMMENT ON COLUMN annotation.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN annotation.annotation_object_id
IS 'internal Documentum primary key from moj_annotation_s';

COMMENT ON COLUMN annotation.annotation_text
IS 'directly sourced from moj_annotation_s.c_text';

COMMENT ON COLUMN annotation.annotation_ts
IS 'directly sourced from moj_annotation_s';

COMMENT ON COLUMN annotation.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_annotation';


CREATE TABLE audit
(aud_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,aua_id                      INTEGER                       NOT NULL
,usr_id                      INTEGER                       NOT NULL
,application_server          CHARACTER VARYING             NOT NULL
,additional_data             CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL

) TABLESPACE darts_tables;

COMMENT ON COLUMN audit.aud_id
IS 'primary key of audit';

COMMENT ON COLUMN audit.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN audit.aua_id
IS 'foreign key from audit_activity';

COMMENT ON COLUMN audit.usr_id
IS 'foreign key from user_account';


CREATE TABLE audit_activity
(aua_id                       INTEGER                      NOT NULL
,activity_name                CHARACTER VARYING            NOT NULL
,activity_description         CHARACTER VARYING            NOT NULL
,created_ts                   TIMESTAMP WITH TIME ZONE     NOT NULL
,created_by                   INTEGER                      NOT NULL
,last_modified_ts             TIMESTAMP WITH TIME ZONE     NOT NULL
,last_modified_by             INTEGER                      NOT NULL

) TABLESPACE darts_tables;

COMMENT ON COLUMN audit.aua_id
IS 'primary key of audit_activity';


CREATE TABLE automated_task
(aut_id                      INTEGER                       NOT NULL
,task_name                   CHARACTER VARYING             NOT NULL
,task_description            CHARACTER VARYING             NOT NULL
,cron_expression             CHARACTER VARYING             NOT NULL
,cron_editable               BOOLEAN                       NOT NULL
,task_enabled                BOOLEAN                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN automated_task.aut_id
IS 'primary key of automated_task';


CREATE TABLE case_judge_ae
(cas_id                      INTEGER                       NOT NULL
,jud_id                      INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN case_judge_ae.cas_id
IS 'foreign key from case, part of composite natural key and PK';

COMMENT ON COLUMN case_judge_ae.jud_id
IS 'foreign key from judge, part of composite natural key and PK';


CREATE TABLE case_retention
(car_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,rtp_id                      INTEGER                       NOT NULL
,retain_until_ts             TIMESTAMP WITH TIME ZONE
,manual_override             BOOLEAN                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL          
) TABLESPACE darts_tables;

COMMENT ON COLUMN case_retention.car_id
IS 'primary key of case_retention';

COMMENT ON COLUMN case_retention.cas_id
IS 'foreign key from court_case';

COMMENT ON COLUMN case_retention.rtp_id
IS 'foreign key from retention_policy';


CREATE TABLE case_retention_event
(cre_id                      INTEGER                       NOT NULL
,car_id                      INTEGER                       NOT NULL
,sentencing_type             INTEGER                       NOT NULL
,total_sentencing            CHARACTER VARYING
,last_processed_event_ts     TIMESTAMP WITH TIME ZONE      NOT NULL
,submitted_by                INTEGER
,user_comment                CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL      
) TABLESPACE darts_tables;

COMMENT ON COLUMN case_retention_event.cre_id
IS 'primary key of case_retention_event';

COMMENT ON COLUMN case_retention_event.car_id
IS 'foreign key from case_retention';


CREATE TABLE court_case
(cas_id                      INTEGER                       NOT NULL
,cth_id                      INTEGER                       NOT NULL
,evh_id                      INTEGER               -- must map to one of the reporting restriction elements found on event_handler
,case_object_id              CHARACTER VARYING(16)
,case_number                 CHARACTER VARYING     -- maps to c_case_id in legacy                    
,case_closed                 BOOLEAN
,interpreter_used            BOOLEAN
,case_closed_ts              TIMESTAMP WITH TIME ZONE
,retain_until_ts             TIMESTAMP WITH TIME ZONE
,version_label               CHARACTER VARYING(32)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN court_case.cas_id
IS 'primary key of court_case';

COMMENT ON COLUMN court_case.cth_id
IS 'foreign key to courthouse';

COMMENT ON COLUMN court_case.case_object_id
IS 'internal Documentum primary key from moj_case_s';

COMMENT ON COLUMN court_case.case_number
IS 'directly sourced from moj_case_s.c_case_id';

COMMENT ON COLUMN court_case.case_closed
IS 'migrated from moj_case_s, converted from numeric to boolean';

COMMENT ON COLUMN court_case.interpreter_used
IS 'migrated from moj_case_s, converted from numeric to boolean';

COMMENT ON COLUMN court_case.case_closed_ts
IS 'directly sourced from moj_case_s.c_case_closed_date';

COMMENT ON COLUMN court_case.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_case, containing the version record';

CREATE TABLE courthouse
(cth_id                      INTEGER                       NOT NULL
,courthouse_code             INTEGER                       NOT NULL          UNIQUE
,courthouse_name             CHARACTER VARYING             NOT NULL          UNIQUE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN courthouse.cth_id
IS 'primary key of courthouse';

COMMENT ON COLUMN courthouse.courthouse_code
IS 'corresponds to the c_crown_court_code found in daily lists';

COMMENT ON COLUMN courthouse.courthouse_name
IS 'directly sourced from moj_courthouse_s.c_id';

CREATE TABLE courthouse_region_ae
(cth_id                      INTEGER                       NOT NULL
,reg_id                      INTEGER                       NOT NULL
) TABLESPACE darts_tables;

CREATE TABLE courtroom
(ctr_id                      INTEGER                       NOT NULL
,cth_id                      INTEGER                       NOT NULL
,courtroom_name              CHARACTER VARYING             NOT NULL
--,UNIQUE(moj_cth_id,courtroom_name)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN courtroom.ctr_id
IS 'primary key of courtroom';

COMMENT ON COLUMN courtroom.cth_id
IS 'foreign key to courthouse';

CREATE TABLE daily_list
(dal_id                      INTEGER                       NOT NULL
,cth_id                      INTEGER                       NOT NULL
,daily_list_object_id        CHARACTER VARYING(16)
,unique_id                   CHARACTER VARYING
--,c_crown_court_name        CHARACTER VARYING        -- removed, normalised to courthouses, but note that in legacy there is mismatch between moj_courthouse_s.c_id and moj_daily_list_s.c_crown_court_name to be resolved
,job_status                  CHARACTER VARYING        -- one of "New","Partially Processed","Processed","Ignored","Invalid"
,published_ts                TIMESTAMP WITH TIME ZONE 
,start_dt                    DATE   
,end_dt                      DATE -- all values match c_start_date
,daily_list_id_s             CHARACTER VARYING        -- non unique integer in legacy
,daily_list_source           CHARACTER VARYING        -- one of CPP,XHB ( live also sees nulls and spaces)   
,daily_list_content_json     CHARACTER VARYING
,daily_list_content_xml      CHARACTER VARYING
,version_label               CHARACTER VARYING(32)  
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN daily_list.dal_id
IS 'primary key of daily_list';

COMMENT ON COLUMN daily_list.cth_id
IS 'foreign key from courthouse';

COMMENT ON COLUMN daily_list.daily_list_object_id
IS 'internal Documentum primary key from moj_daily_list_s';

COMMENT ON COLUMN daily_list.unique_id
IS 'directly sourced from moj_daily_list_s, received as part of the XML, used to find duplicate daily lists';

COMMENT ON COLUMN daily_list.job_status
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN daily_list.published_ts
IS 'directly sourced from moj_daily_list_s.c_timestamp';

COMMENT ON COLUMN daily_list.start_dt
IS 'directly sourced from moj_daily_list_s.c_start_date';

COMMENT ON COLUMN daily_list.end_dt
IS 'directly sourced from moj_daily_list_s.c_end_date';

COMMENT ON COLUMN daily_list.daily_list_id_s
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN daily_list.daily_list_source
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN daily_list.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_daily_list';

CREATE TABLE defence
(dfc_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,defence_name                CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN defence.dfc_id 
IS 'primary key of defence';

COMMENT ON COLUMN defence.cas_id
IS 'foreign key from court_case';

CREATE TABLE defendant
(dfd_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,defendant_name              CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN defendant.dfd_id 
IS 'primary key of defendant';

COMMENT ON COLUMN defendant.cas_id
IS 'foreign key from court_case';

CREATE TABLE device_register
(node_id                     INTEGER                       NOT NULL  --pk column breaks pattern used, is not der_id
,ctr_id                      INTEGER                       NOT NULL
,device_type                 CHARACTER VARYING             NOT NULL
,hostname                    CHARACTER VARYING             NOT NULL
,ip_address                  CHARACTER VARYING             NOT NULL
,mac_address                 CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON TABLE device_register
IS 'corresponds to tbl_moj_node from legacy';

COMMENT ON COLUMN device_register.node_id
IS 'primary key of device_register';

COMMENT ON COLUMN device_register.ctr_id 
IS 'foreign key from moj_courtroom, legacy stored courthouse and courtroon un-normalised';


CREATE TABLE event
(eve_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER
,evh_id                      INTEGER
,event_object_id             CHARACTER VARYING(16)
,event_id                    INTEGER
,event_name                  CHARACTER VARYING -- details of the handler, at point in time the event arose, lots of discussion re import of legacy, retain.
,event_text                  CHARACTER VARYING
,event_ts                    TIMESTAMP WITH TIME ZONE  
,case_number                 CHARACTER VARYING(32)[] 
,message_id                  CHARACTER VARYING
,is_log_entry                BOOLEAN                       NOT NULL  -- needs to be not null to ensure only 2 valid states
,version_label               CHARACTER VARYING(32)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN event.eve_id
IS 'primary key of moj_event';

COMMENT ON COLUMN event.ctr_id
IS 'foreign key from moj_courtroom';

COMMENT ON COLUMN event.evh_id
IS 'foreign key for the moj_event_handler table';

COMMENT ON COLUMN event.event_object_id
IS 'internal Documentum primary key from moj_event_s';

COMMENT ON COLUMN event.event_id
IS 'directly sourced from moj_event_s';

COMMENT ON COLUMN event.event_name
IS 'inherited from dm_sysobect_s.object_name';

COMMENT ON COLUMN event.event_text
IS 'inherited from moj_annotation_s.c_text';

COMMENT ON COLUMN event.event_ts
IS 'inherited from moj_annotation_s';

COMMENT ON COLUMN event.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_event';

COMMENT ON COLUMN event.message_id
IS 'no migration element, records the id of the message that gave rise to this event';

CREATE TABLE event_handler
(evh_id                      INTEGER                       NOT NULL
,event_type                  CHARACTER VARYING             NOT NULL
,event_sub_type              CHARACTER VARYING
,event_name                  CHARACTER VARYING             NOT NULL
,handler                     CHARACTER VARYING
,active                      BOOLEAN                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON TABLE event_handler
IS 'content will be derived from TBL_MOJ_DOC_HANDLER in the legacy database, but currently has no primary key and 6 fully duplicated rows';

COMMENT ON COLUMN event_handler.evh_id
IS 'primary key of moj_event_type';

COMMENT ON COLUMN event_handler.event_type
IS 'directly sourced from doc_type';

COMMENT ON COLUMN event_handler.event_sub_type
IS 'directly sourced from doc_sub_type';

COMMENT ON COLUMN event_handler.event_name
IS 'directly sourced from event_name';

COMMENT ON COLUMN event_handler.handler
IS 'directly sourced from doc_handler';


CREATE TABLE external_object_directory
(eod_id                      INTEGER                       NOT NULL
,med_id                      INTEGER
,tra_id                      INTEGER
,ann_id                      INTEGER
,ods_id                      INTEGER                       NOT NULL  -- FK to object_directory_status
,elt_id                      INTEGER                       NOT NULL  -- FK to external_location_type 
-- additional optional FKs to other relevant internal objects would require columns here
,external_location           UUID                          NOT NULL
,external_file_id            CHARACTER VARYING  
,external_record_id          CHARACTER VARYING                       -- for use where address of Ext Obj requires 2 fields
,checksum                    CHARACTER VARYING                       -- for use where address of Ext Obj requires 2 fields
,transfer_attempts           INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN external_object_directory.eod_id
IS 'primary key of external_object_directory';

COMMENT ON COLUMN external_object_directory.elt_id
IS 'foreign key from external_location_type';

-- added two foreign key columns, but there will be as many FKs as there are distinct objects with externally stored components

COMMENT ON COLUMN external_object_directory.med_id
IS 'foreign key from media';

COMMENT ON COLUMN external_object_directory.tra_id
IS 'foreign key from transcription';

COMMENT ON COLUMN external_object_directory.ann_id
IS 'foreign key from annotation';


CREATE TABLE external_location_type
(elt_id                      INTEGER                       NOT NULL
,elt_description             CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON TABLE external_location_type
IS 'used to record acceptable external locations, found in external_object_directory';


CREATE TABLE external_service_auth_token
(esa_id                      INTEGER                       NOT NULL
,external_service_userid     CHARACTER VARYING             NOT NULL
,token_type                  INTEGER                       NOT NULL
,token                       CHARACTER VARYING             NOT NULL
,expiry_ts                   TIMESTAMP WITH TIME ZONE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL  
) TABLESPACE darts_tables;

COMMENT ON COLUMN external_service_auth_token.esa_id
IS 'primary key of external_service_auth_token';

COMMENT ON COLUMN external_service_auth_token.external_service_userid
IS 'userId used by external services OR requestId user by Audio Request';

COMMENT ON COLUMN external_service_auth_token.token_type
IS '1=soap token, 2=email link token';

COMMENT ON COLUMN external_service_auth_token.token
IS 'Encrypted Token';

COMMENT ON COLUMN external_service_auth_token.expiry_ts
IS 'Expiry Date & Time of the Token';


CREATE TABLE hearing
(hea_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER                       NOT NULL
,hearing_date                DATE     -- to record only DATE component of hearings, both scheduled and actual
,scheduled_start_time        TIME     -- to record only TIME component of hearings, while they are scheduled only
,hearing_is_actual           BOOLEAN  -- TRUE for actual hearings, FALSE for scheduled hearings
,judge_hearing_date          CHARACTER VARYING
--,UNIQUE(moj_cas_id,moj_ctr,c_hearing_date)
,created_ts                  TIMESTAMP WITH TIME ZONE       NOT NULL
,created_by                  INTEGER                        NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE       NOT NULL
,last_modified_by            INTEGER                        NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN hearing.hea_id
IS 'primary key of hearing';

COMMENT ON COLUMN hearing.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN hearing.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN hearing.hearing_date
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN hearing.judge_hearing_date
IS 'directly sourced from moj_case_r';

CREATE TABLE hearing_event_ae
(hea_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN hearing_event_ae.hea_id
IS 'foreign key from hearing, part of composite natural key and PK';

COMMENT ON COLUMN hearing_event_ae.eve_id
IS 'foreign key from event, part of composite natural key and PK';

CREATE TABLE hearing_judge_ae
(hea_id                      INTEGER                       NOT NULL
,jud_id                      INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN hearing_judge_ae.hea_id
IS 'foreign key from case, part of composite natural key and PK';

COMMENT ON COLUMN hearing_judge_ae.jud_id
IS 'foreign key from judge, part of composite natural key and PK';

CREATE TABLE hearing_media_ae
(hea_id                      INTEGER                       NOT NULL
,med_id                      INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN hearing_media_ae.hea_id
IS 'foreign key from case, part of composite natural key and PK';

COMMENT ON COLUMN hearing_media_ae.med_id
IS 'foreign key from media, part of composite natural key and PK';

CREATE TABLE judge
(jud_id                      INTEGER                       NOT NULL
,judge_name                  CHARACTER VARYING             NOT NULL          UNIQUE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN judge.jud_id 
IS 'primary key of judge';

CREATE TABLE media
(med_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER
,media_object_id             CHARACTER VARYING(16)
,channel                     INTEGER
,total_channels              INTEGER                       --99.9% are "4" in legacy 
,reference_id                CHARACTER VARYING             --all nulls in legacy
,start_ts                    TIMESTAMP WITH TIME ZONE 
,end_ts                      TIMESTAMP WITH TIME ZONE
,case_number                 CHARACTER VARYING(32)[]       --this is a placeholder for moj_case_document_r.c_case_id, known to be repeated for moj_media object types
,version_label               CHARACTER VARYING(32)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN media.med_id
IS 'primary key of media';

COMMENT ON COLUMN media.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN media.media_object_id
IS 'internal Documentum primary key from moj_media_s';

COMMENT ON COLUMN media.channel
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN media.total_channels
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN media.reference_id
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN media.start_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN media.end_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN media.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_media';

CREATE TABLE media_request
(mer_id                      INTEGER                       NOT NULL
,hea_id                      INTEGER                       NOT NULL
,requestor                   INTEGER                       NOT NULL  -- FK to user_account
,current_owner               INTEGER                       NOT NULL  -- FK to user_account
,request_status              CHARACTER VARYING             NOT NULL
,request_type                CHARACTER VARYING             NOT NULL
,req_proc_attempts           INTEGER 
,start_ts                    TIMESTAMP WITH TIME ZONE      NOT NULL
,end_ts                      TIMESTAMP WITH TIME ZONE      NOT NULL
,last_accessed_ts            TIMESTAMP WITH TIME ZONE
,expiry_ts                   TIMESTAMP WITH TIME ZONE
,output_filename             CHARACTER VARYING
,output_format               CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN media_request.mer_id
IS 'primary key of media_request';

COMMENT ON COLUMN media_request.hea_id
IS 'foreign key of hearing';

COMMENT ON COLUMN media_request.requestor
IS 'requestor of the media request, possibly migrated from moj_transformation_request_s';

COMMENT ON COLUMN media_request.request_status
IS 'status of the media request';

COMMENT ON COLUMN media_request.req_proc_attempts
IS 'number of attempts by ATS to process the request';

COMMENT ON COLUMN media_request.start_ts
IS 'start time in the search criteria for request, possibly migrated from moj_cached_media_s or moj_transformation_request_s';

COMMENT ON COLUMN media_request.end_ts
IS 'end time in the search criteria for request, possibly migrated from moj_cached_media_s or moj_transformation_request_s';

COMMENT ON COLUMN media_request.output_filename
IS 'filename of the requested media object, possibly migrated from moj_transformation_request_s';

COMMENT ON COLUMN media_request.output_format
IS 'format of the requested media object, possibly migrated from moj_transformation_s';


CREATE TABLE notification
(not_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,notification_event          CHARACTER VARYING             NOT NULL
,notification_status         CHARACTER VARYING             NOT NULL
,email_address               CHARACTER VARYING             NOT NULL
,send_attempts               INTEGER
,template_values             CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN notification.not_id
IS 'primary key of notification';

COMMENT ON COLUMN notification.cas_id
IS 'foreign key to case';

COMMENT ON COLUMN notification.notification_event
IS 'event giving rise to the need for outgoing notification';

COMMENT ON COLUMN notification.notification_status
IS 'status of the notification, expected to be one of [O]pen, [P]rocessing, [S]end, [F]ailed';

COMMENT ON COLUMN notification.email_address
IS 'recipient of the notification';

COMMENT ON COLUMN notification.send_attempts
IS 'number of outgoing requests to gov.uk';

COMMENT ON COLUMN notification.template_values
IS 'any extra fields not already covered or inferred from the case, in JSON format';


CREATE TABLE object_directory_status
(ods_id                      INTEGER                       NOT NULL
,ods_description             CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON TABLE object_directory_status
IS 'used to record acceptable statuses found in [external/transient]_object_directory';

CREATE TABLE prosecutor
(prn_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,prosecutor_name             CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN prosecutor.prn_id 
IS 'primary key of prosecutor';

COMMENT ON COLUMN prosecutor.cas_id
IS 'foreign key from court_case';

CREATE TABLE region
(reg_id                      INTEGER                       NOT NULL
,region_name                 CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;


CREATE TABLE report               
(rep_id                      INTEGER                       NOT NULL
,report_object_id            CHARACTER VARYING(16)
,name                        CHARACTER VARYING
,subject                     CHARACTER VARYING
,report_text                 CHARACTER VARYING
,query                       CHARACTER VARYING
,recipients                  CHARACTER VARYING
,version_label               CHARACTER VARYING(32)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN report.rep_id
IS 'primary key of report';

COMMENT ON COLUMN report.report_object_id
IS 'internal Documentum primary key from moj_report_s';

COMMENT ON COLUMN report.name
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN report.subject
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN report.report_text
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN report.query
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN report.recipients
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN report.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_report';

CREATE TABLE retention_policy
(rtp_id                      INTEGER                       NOT NULL
,policy_name                 CHARACTER VARYING             NOT NULL
,retention_period            INTEGER                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN retention_policy.rtp_id
IS 'primary key of retention_policy';

CREATE TABLE transcription
(tra_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,trt_id                      INTEGER                       NOT NULL
,trs_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER                  
,tru_id                      INTEGER                                -- remains nullable, as nulls present in source data ( c_urgency)       
,hea_id                      INTEGER                                -- remains nullable, until migration is complete
,transcription_object_id     CHARACTER VARYING(16)                  -- legacy pk from moj_transcription_s.r_object_id
,company                     CHARACTER VARYING                      -- effectively unused in legacy, either null or "<this field will be completed by the system>"
,requestor                   CHARACTER VARYING                      -- 1055 distinct, from <forname><surname> to <AAANNA>
,current_state_ts            TIMESTAMP WITH TIME ZONE               -- date & time record entered the current c_current_state
,hearing_date                TIMESTAMP WITH TIME ZONE               -- 3k records have time component, but all times are 23:00,so effectively DATE only, will be absolete once moj_hea_id populated
,start_ts                    TIMESTAMP WITH TIME ZONE               -- both c_start and c_end have time components
,end_ts                      TIMESTAMP WITH TIME ZONE               -- we have 49k rows in legacy moj_transcription_s, 7k have c_end != c_start
,version_label               CHARACTER VARYING(32)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN transcription.tra_id
IS 'primary key of transcription';
    
COMMENT ON COLUMN transcription.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN transcription.trt_id
IS 'foreign key to transcription_type, sourced from moj_transcription_s.c_type';

COMMENT ON COLUMN transcription.trs_id
IS 'foreign key to transcription_status';

COMMENT ON COLUMN transcription.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN transcription.tru_id
IS 'foreign key from transcription_urgency';

COMMENT ON COLUMN transcription.transcription_object_id
IS 'internal Documentum primary key from moj_transcription_s';
    
COMMENT ON COLUMN transcription.company
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN transcription.requestor
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN transcription.hearing_date
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN transcription.start_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN transcription.end_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN transcription.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_transcription';

CREATE TABLE transcription_comment
(trc_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER
,transcription_object_id     CHARACTER VARYING(16)         -- this is a placeholder for moj_transcription_s.r_object_id
,transcription_comment       CHARACTER VARYING
,comment_ts                  TIMESTAMP WITH TIME ZONE
,author                      INTEGER                       -- will need to be FK to user table
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN transcription_comment.trc_id
IS 'primary key of transcription_comment'; 

COMMENT ON COLUMN transcription_comment.tra_id
IS 'foreign key from transcription'; 

COMMENT ON COLUMN transcription_comment.transcription_object_id
IS 'internal Documentum primary key from moj_transcription_s'; 

COMMENT ON COLUMN transcription_comment.transcription_comment
IS 'directly sourced from moj_transcription_r';

COMMENT ON COLUMN transcription_comment.transcription_object_id
IS 'internal Documentum id from moj_transcription_s acting as foreign key';

CREATE TABLE transcription_status
(trs_id                      INTEGER                       NOT NULL
,status_type                 CHARACTER VARYING             NOT NULL
);

COMMENT ON TABLE transcription_status
IS 'standing data table';

COMMENT ON COLUMN transcription_status.trs_id
IS 'primary key of transcription_status';


CREATE TABLE transcription_type
(trt_id                      INTEGER                       NOT NULL
,description                 CHARACTER VARYING        
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
);

COMMENT ON TABLE transcription_type
IS 'standing data table, migrated from tbl_moj_transcription_type';

COMMENT ON COLUMN transcription_type.trt_id
IS 'primary key, but not sequence generated';


CREATE TABLE transcription_urgency
(tru_id                      INTEGER                       NOT NULL
,description                 CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

COMMENT ON TABLE transcription_urgency 
IS 'will be migrated from tbl_moj_urgency';

COMMENT ON COLUMN transcription_urgency.tru_id 
IS 'inherited from tbl_moj_urgency.urgency_id';

COMMENT ON COLUMN transcription_urgency.description
IS 'inherited from tbl_moj_urgency.description';

CREATE TABLE transcription_workflow
(trw_id                      INTEGER                       NOT NULL 
,tra_id                      INTEGER                       NOT NULL  -- FK to transcription 
,workflow_stage              CHARACTER VARYING             NOT NULL  -- will include REQUEST, APPROVAL etc
,workflow_comment            CHARACTER VARYING             
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;



CREATE TABLE transient_object_directory
(tod_id                      INTEGER                       NOT NULL
,mer_id                      INTEGER                       NOT NULL 
,ods_id                      INTEGER                       NOT NULL  -- FK to moj_object_directory_status.moj_ods_id
,external_location           UUID                          NOT NULL
,checksum                    CHARACTER VARYING
,transfer_attempts           INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL                    
) TABLESPACE darts_tables;


CREATE TABLE user_account
(usr_id                      INTEGER                       NOT NULL
,dm_user_s_object_id         CHARACTER VARYING(16)
,user_name                   CHARACTER VARYING
,user_email_address          CHARACTER VARYING
,description                 CHARACTER VARYING
,user_state                  INTEGER
,last_login_ts               TIMESTAMP WITH TIME ZONE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL                    
) TABLESPACE darts_tables;

COMMENT ON TABLE user_account 
IS 'migration columns all sourced directly from dm_user_s, but only for rows where r_is_group = 0';
COMMENT ON COLUMN user_account.usr_id
IS 'primary key of user_account';
COMMENT ON COLUMN user_account.dm_user_s_object_id
IS 'internal Documentum primary key from dm_user_s';


-- primary keys

CREATE UNIQUE INDEX annotation_pk ON annotation(ann_id) TABLESPACE darts_indexes;
ALTER TABLE annotation              ADD PRIMARY KEY USING INDEX annotation_pk;

CREATE UNIQUE INDEX audit_pk ON audit(aud_id) TABLESPACE darts_indexes;
ALTER TABLE audit          ADD PRIMARY KEY USING INDEX audit_pk;

CREATE UNIQUE INDEX audit_activity_pk ON audit_activity(aua_id) TABLESPACE darts_indexes;
ALTER TABLE audit_activity          ADD PRIMARY KEY USING INDEX audit_activity_pk;

CREATE UNIQUE INDEX automated_task_pk ON automated_task(aut_id) TABLESPACE darts_indexes;
ALTER TABLE automated_task          ADD PRIMARY KEY USING INDEX automated_task_pk;

CREATE UNIQUE INDEX case_judge_ae_pk ON case_judge_ae(cas_id,jud_id) TABLESPACE darts_indexes;
ALTER TABLE case_judge_ae        ADD PRIMARY KEY USING INDEX case_judge_ae_pk;

CREATE UNIQUE INDEX case_retention_pk ON case_retention(car_id) TABLESPACE darts_indexes; 
ALTER TABLE case_retention          ADD PRIMARY KEY USING INDEX case_retention_pk;

CREATE UNIQUE INDEX case_retention_event_pk ON case_retention_event(cre_id) TABLESPACE darts_indexes; 
ALTER TABLE case_retention_event    ADD PRIMARY KEY USING INDEX case_retention_event_pk;

CREATE UNIQUE INDEX court_case_pk ON court_case(cas_id) TABLESPACE darts_indexes; 
ALTER TABLE court_case              ADD PRIMARY KEY USING INDEX court_case_pk;

CREATE UNIQUE INDEX courthouse_pk ON courthouse(cth_id) TABLESPACE darts_indexes;
ALTER TABLE courthouse              ADD PRIMARY KEY USING INDEX courthouse_pk;

CREATE UNIQUE INDEX courthouse_region_ae_pk ON courthouse_region_ae(cth_id,reg_id) TABLESPACE darts_indexes;
ALTER TABLE courthouse_region_ae    ADD PRIMARY KEY USING INDEX courthouse_region_ae_pk;

CREATE UNIQUE INDEX courtroom_pk ON courtroom(ctr_id) TABLESPACE darts_indexes;
ALTER TABLE courtroom               ADD PRIMARY KEY USING INDEX courtroom_pk;

CREATE UNIQUE INDEX daily_list_pk ON daily_list(dal_id) TABLESPACE darts_indexes;
ALTER TABLE daily_list              ADD PRIMARY KEY USING INDEX daily_list_pk;

CREATE UNIQUE INDEX defence_pk    ON defence(dfc_id) TABLESPACE darts_indexes;
ALTER TABLE defence               ADD PRIMARY KEY USING INDEX defence_pk;

CREATE UNIQUE INDEX defendant_pk ON defendant(dfd_id) TABLESPACE darts_indexes;
ALTER TABLE defendant             ADD PRIMARY KEY USING INDEX defendant_pk;

CREATE UNIQUE INDEX device_register_pk ON device_register(node_id) TABLESPACE darts_indexes;
ALTER TABLE device_register         ADD PRIMARY KEY USING INDEX device_register_pk;

CREATE UNIQUE INDEX event_pk ON event(eve_id) TABLESPACE darts_indexes;
ALTER TABLE event                   ADD PRIMARY KEY USING INDEX event_pk;

CREATE UNIQUE INDEX event_handler_pk ON event_handler(evh_id) TABLESPACE darts_indexes;
ALTER TABLE event_handler            ADD PRIMARY KEY USING INDEX event_handler_pk;

CREATE UNIQUE INDEX external_object_directory_pk ON external_object_directory(eod_id) TABLESPACE darts_indexes;
ALTER TABLE external_object_directory   ADD PRIMARY KEY USING INDEX external_object_directory_pk;

CREATE UNIQUE INDEX external_location_type_pk ON external_location_type(elt_id) TABLESPACE darts_indexes;
ALTER TABLE external_location_type   ADD PRIMARY KEY USING INDEX external_location_type_pk;

CREATE UNIQUE INDEX external_service_auth_token_pk ON external_service_auth_token(esa_id) TABLESPACE darts_indexes;
ALTER TABLE external_service_auth_token   ADD PRIMARY KEY USING INDEX external_service_auth_token_pk;

CREATE UNIQUE INDEX hearing_pk ON hearing(hea_id) TABLESPACE darts_indexes;
ALTER TABLE hearing                 ADD PRIMARY KEY USING INDEX hearing_pk;

CREATE UNIQUE INDEX hearing_event_ae_pk ON hearing_event_ae(hea_id,eve_id) TABLESPACE darts_indexes;
ALTER TABLE hearing_event_ae        ADD PRIMARY KEY USING INDEX hearing_event_ae_pk;

CREATE UNIQUE INDEX hearing_judge_ae_pk ON hearing_judge_ae(hea_id,jud_id) TABLESPACE darts_indexes;
ALTER TABLE hearing_judge_ae        ADD PRIMARY KEY USING INDEX hearing_judge_ae_pk;

CREATE UNIQUE INDEX hearing_media_ae_pk ON hearing_media_ae(hea_id,med_id) TABLESPACE darts_indexes;
ALTER TABLE hearing_media_ae        ADD PRIMARY KEY USING INDEX hearing_media_ae_pk;

CREATE UNIQUE INDEX judge_pk     ON judge(jud_id) TABLESPACE darts_indexes;
ALTER TABLE judge                ADD PRIMARY KEY USING INDEX judge_pk;

CREATE UNIQUE INDEX media_pk ON media(med_id) TABLESPACE darts_indexes;
ALTER TABLE media                   ADD PRIMARY KEY USING INDEX media_pk;

CREATE UNIQUE INDEX media_request_pk ON media_request(mer_id) TABLESPACE darts_indexes;
ALTER TABLE media_request           ADD PRIMARY KEY USING INDEX media_request_pk;

CREATE UNIQUE INDEX notification_pk ON notification(not_id) TABLESPACE darts_indexes;
ALTER TABLE notification            ADD PRIMARY KEY USING INDEX notification_pk;

CREATE UNIQUE INDEX object_directory_status_pk ON object_directory_status(ods_id) TABLESPACE darts_indexes;
ALTER TABLE object_directory_status ADD PRIMARY KEY USING INDEX object_directory_status_pk;

CREATE UNIQUE INDEX prosecutor_pk ON prosecutor(prn_id) TABLESPACE darts_indexes;
ALTER TABLE prosecutor          ADD PRIMARY KEY USING INDEX prosecutor_pk;

CREATE UNIQUE INDEX region_pk ON region(reg_id) TABLESPACE darts_indexes;
ALTER TABLE region                  ADD PRIMARY KEY USING INDEX region_pk;

CREATE UNIQUE INDEX report_pk ON report(rep_id) TABLESPACE darts_indexes;
ALTER TABLE report                  ADD PRIMARY KEY USING INDEX report_pk;

CREATE UNIQUE INDEX retention_policy_pk ON retention_policy(rtp_id) TABLESPACE darts_indexes;
ALTER TABLE retention_policy           ADD PRIMARY KEY USING INDEX retention_policy_pk;

CREATE UNIQUE INDEX transcription_pk ON transcription(tra_id) TABLESPACE darts_indexes;
ALTER TABLE transcription           ADD PRIMARY KEY USING INDEX transcription_pk;

CREATE UNIQUE INDEX transcription_comment_pk ON transcription_comment(trc_id) TABLESPACE darts_indexes;
ALTER TABLE transcription_comment   ADD PRIMARY KEY USING INDEX transcription_comment_pk;

CREATE UNIQUE INDEX transcription_status_pk ON transcription_status(trs_id) TABLESPACE darts_indexes;
ALTER TABLE transcription_status      ADD PRIMARY KEY USING INDEX transcription_status_pk;

CREATE UNIQUE INDEX transcription_type_pk ON transcription_type(trt_id) TABLESPACE darts_indexes;
ALTER TABLE transcription_type      ADD PRIMARY KEY USING INDEX transcription_type_pk;

CREATE UNIQUE INDEX transcription_urgency_pk ON transcription_urgency(tru_id) TABLESPACE darts_indexes;
ALTER TABLE transcription_urgency                 ADD PRIMARY KEY USING INDEX transcription_urgency_pk;

CREATE UNIQUE INDEX transcription_workflow_pk ON transcription_workflow( trw_id) TABLESPACE darts_indexes;
ALTER TABLE transcription_workflow               ADD PRIMARY KEY USING INDEX transcription_workflow_pk;

CREATE UNIQUE INDEX transient_object_directory_pk ON transient_object_directory(tod_id) TABLESPACE darts_indexes;
ALTER TABLE transient_object_directory  ADD PRIMARY KEY USING INDEX transient_object_directory_pk;

CREATE UNIQUE INDEX user_account_pk ON user_account( usr_id) TABLESPACE darts_indexes;
ALTER TABLE user_account            ADD PRIMARY KEY USING INDEX user_account_pk;



-- defaults for postgres sequences, datatype->bigint, increment->1, nocycle is default, owned by none
CREATE SEQUENCE ann_seq CACHE 20;
CREATE SEQUENCE aud_seq CACHE 20;
CREATE SEQUENCE aua_seq CACHE 20 RESTART WITH 8;
CREATE SEQUENCE aut_seq CACHE 20;
CREATE SEQUENCE car_seq CACHE 20;
CREATE SEQUENCE cre_seq CACHE 20;
CREATE SEQUENCE cas_seq CACHE 20;
CREATE SEQUENCE cth_seq CACHE 20;
CREATE SEQUENCE ctr_seq CACHE 20;
CREATE SEQUENCE dal_seq CACHE 20;
CREATE SEQUENCE dfc_seq CACHE 20;
CREATE SEQUENCE dfd_seq CACHE 20;
CREATE SEQUENCE der_seq CACHE 20 START WITH 50000;   -- sequence for device_register.node_id
CREATE SEQUENCE eve_seq CACHE 20;
CREATE SEQUENCE evh_seq CACHE 20;
CREATE SEQUENCE eod_seq CACHE 20;
CREATE SEQUENCE elt_seq CACHE 20;
CREATE SEQUENCE esa_seq CACHE 20;
CREATE SEQUENCE jud_seq CACHE 20;
CREATE SEQUENCE hea_seq CACHE 20;
CREATE SEQUENCE med_seq CACHE 20;
CREATE SEQUENCE mer_seq CACHE 20;
CREATE SEQUENCE not_seq CACHE 20;
CREATE SEQUENCE ods_seq CACHE 20;
CREATE SEQUENCE prn_seq CACHE 20;
CREATE SEQUENCE reg_seq CACHE 20;
CREATE SEQUENCE rep_seq CACHE 20;
CREATE SEQUENCE rtp_seq CACHE 20;
CREATE SEQUENCE tod_seq CACHE 20;
CREATE SEQUENCE tra_seq CACHE 20;
CREATE SEQUENCE trc_seq CACHE 20;
CREATE SEQUENCE trs_seq CACHE 20;
CREATE SEQUENCE trt_seq CACHE 20;
CREATE SEQUENCE tru_seq CACHE 20;
CREATE SEQUENCE trw_seq CACHE 20;
CREATE SEQUENCE usr_seq CACHE 20;


-- foreign keys

ALTER TABLE annotation                
ADD CONSTRAINT annotation_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE annotation                
ADD CONSTRAINT annotation_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE annotation   
ADD CONSTRAINT annotation_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation   
ADD CONSTRAINT annotation_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);


ALTER TABLE audit                
ADD CONSTRAINT audit_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE audit                
ADD CONSTRAINT audit_audit_activity_fk
FOREIGN KEY (aua_id) REFERENCES audit_activity(aua_id);

ALTER TABLE audit                
ADD CONSTRAINT audit_user_account_fk
FOREIGN KEY (usr_id) REFERENCES user_account(usr_id);

ALTER TABLE audit   
ADD CONSTRAINT audit_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE audit   
ADD CONSTRAINT audit_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE audit_activity   
ADD CONSTRAINT audit_activity_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE audit_activity   
ADD CONSTRAINT audit_activity_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE automated_task   
ADD CONSTRAINT automated_task_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE automated_task   
ADD CONSTRAINT automated_task_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_judge_ae            
ADD CONSTRAINT case_judge_ae_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_judge_ae            
ADD CONSTRAINT case_judge_ae_judge_fk
FOREIGN KEY (jud_id) REFERENCES judge(jud_id);

ALTER TABLE case_retention                
ADD CONSTRAINT case_retention_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_retention                
ADD CONSTRAINT case_retention_retention_policy_fk
FOREIGN KEY (rtp_id) REFERENCES retention_policy(rtp_id);

ALTER TABLE case_retention 
ADD CONSTRAINT case_retention_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention   
ADD CONSTRAINT case_retention_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_event               
ADD CONSTRAINT case_retention_event_case_retention_fk
FOREIGN KEY (car_id) REFERENCES case_retention(car_id);

ALTER TABLE case_retention_event 
ADD CONSTRAINT case_retention_event_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_event   
ADD CONSTRAINT case_retention_event_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE court_case                        
ADD CONSTRAINT court_case_event_handler_fk
FOREIGN KEY (evh_id) REFERENCES event_handler(evh_id);

ALTER TABLE court_case                        
ADD CONSTRAINT court_case_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE court_case
ADD CONSTRAINT court_case_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE court_case   
ADD CONSTRAINT court_case_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE courthouse
ADD CONSTRAINT courthouse_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE courthouse   
ADD CONSTRAINT courthouse_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE courthouse_region_ae                        
ADD CONSTRAINT courthouse_region_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE courthouse_region_ae                        
ADD CONSTRAINT courthouse_region_region_fk
FOREIGN KEY (reg_id) REFERENCES region(reg_id);

ALTER TABLE courtroom                   
ADD CONSTRAINT courtroom_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE courtroom
ADD CONSTRAINT courtroom_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE courtroom   
ADD CONSTRAINT courtroom_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE daily_list                  
ADD CONSTRAINT daily_list_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE daily_list
ADD CONSTRAINT daily_list_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE daily_list   
ADD CONSTRAINT daily_list_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE defence                
ADD CONSTRAINT defence_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE defence
ADD CONSTRAINT defence_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE defence   
ADD CONSTRAINT defence_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE defendant                
ADD CONSTRAINT defendant_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE defendant
ADD CONSTRAINT defendant_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE defendant   
ADD CONSTRAINT defendant_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE device_register
ADD CONSTRAINT device_register_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE device_register
ADD CONSTRAINT device_register_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE device_register   
ADD CONSTRAINT device_register_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE event                       
ADD CONSTRAINT event_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE event                       
ADD CONSTRAINT event_event_handler_fk
FOREIGN KEY (evh_id) REFERENCES event_handler(evh_id);

ALTER TABLE event
ADD CONSTRAINT event_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE event   
ADD CONSTRAINT event_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE event_handler
ADD CONSTRAINT event_handler_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE event_handler   
ADD CONSTRAINT event_handler_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_annotation_fk
FOREIGN KEY (ann_id) REFERENCES annotation(ann_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT external_object_directory_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES object_directory_status(ods_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_external_location_type_fk
FOREIGN KEY (elt_id) REFERENCES external_location_type(elt_id);

ALTER TABLE external_service_auth_token
ADD CONSTRAINT external_service_auth_token_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE external_service_auth_token   
ADD CONSTRAINT external_service_auth_token_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE hearing                     
ADD CONSTRAINT hearing_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE hearing                     
ADD CONSTRAINT hearing_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE hearing
ADD CONSTRAINT hearing_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE hearing   
ADD CONSTRAINT hearing_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE hearing_event_ae            
ADD CONSTRAINT hearing_event_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_event_ae            
ADD CONSTRAINT hearing_event_ae_event_fk
FOREIGN KEY (eve_id) REFERENCES event(eve_id);

ALTER TABLE hearing_judge_ae            
ADD CONSTRAINT hearing_judge_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_judge_ae            
ADD CONSTRAINT hearing_judge_ae_judge_fk
FOREIGN KEY (jud_id) REFERENCES judge(jud_id);

ALTER TABLE hearing_media_ae            
ADD CONSTRAINT hearing_media_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_media_ae            
ADD CONSTRAINT hearing_media_ae_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE judge
ADD CONSTRAINT judge_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE judge   
ADD CONSTRAINT judge_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE media                       
ADD CONSTRAINT media_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE media
ADD CONSTRAINT media_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE media   
ADD CONSTRAINT media_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE media_request               
ADD CONSTRAINT media_request_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE media_request
ADD CONSTRAINT media_request_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE media_request   
ADD CONSTRAINT media_request_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE media_request   
ADD CONSTRAINT media_request_requestor_fk
FOREIGN KEY (requestor) REFERENCES user_account(usr_id);

ALTER TABLE media_request   
ADD CONSTRAINT media_request_current_owner_fk
FOREIGN KEY (current_owner) REFERENCES user_account(usr_id);

ALTER TABLE notification                
ADD CONSTRAINT notification_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE notification
ADD CONSTRAINT notification_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE notification
ADD CONSTRAINT notification_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE object_directory_status
ADD CONSTRAINT object_directory_status_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE object_directory_status
ADD CONSTRAINT object_directory_status_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE prosecutor               
ADD CONSTRAINT prosecutor_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE prosecutor
ADD CONSTRAINT prosecutor_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE prosecutor
ADD CONSTRAINT prosecutor_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE region
ADD CONSTRAINT region_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE region
ADD CONSTRAINT region_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE report
ADD CONSTRAINT report_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE report
ADD CONSTRAINT report_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE retention_policy
ADD CONSTRAINT retention_policy_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE retention_policy
ADD CONSTRAINT retention_policy_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_urgency_fk
FOREIGN KEY (tru_id) REFERENCES transcription_urgency(tru_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE transcription 
ADD CONSTRAINT transcription_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_transcription_type_fk
FOREIGN KEY (trt_id) REFERENCES transcription_type(trt_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_transcription_status_fk
FOREIGN KEY (trs_id) REFERENCES transcription_status(trs_id);

ALTER TABLE transcription_comment       
ADD CONSTRAINT transcription_comment_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_comment       
ADD CONSTRAINT transcription_comment_author_fk
FOREIGN KEY (author) REFERENCES user_account(usr_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_type
ADD CONSTRAINT transcription_type_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_type
ADD CONSTRAINT transcription_type_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_urgency
ADD CONSTRAINT transcription_urgency_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_urgency
ADD CONSTRAINT transcription_urgency_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory
ADD CONSTRAINT transient_object_directory_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory  
ADD CONSTRAINT tod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory  
ADD CONSTRAINT tod_media_request_fk
FOREIGN KEY (mer_id) REFERENCES media_request(mer_id);

ALTER TABLE transient_object_directory  
ADD CONSTRAINT tod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES object_directory_status(ods_id);



-- set defaults
ALTER TABLE device_register ALTER COLUMN device_type SET DEFAULT 'DAR';

-- additional check constraints

ALTER TABLE courthouse
ADD CONSTRAINT courthouse_name_ck CHECK (courthouse_name = UPPER(courthouse_name));

ALTER TABLE courtroom
ADD CONSTRAINT courtroom_name_ck CHECK (courtroom_name = UPPER(courtroom_name));

ALTER TABLE external_service_auth_token
ADD CONSTRAINT token_type_ck CHECK (token_type in (1,2));

-- additional unique multi-column indexes and constraints

--,UNIQUE (cth_id,courtroom_name)
CREATE UNIQUE INDEX ctr_chr_crn_unq ON courtroom( cth_id, courtroom_name) TABLESPACE darts_indexes;
ALTER TABLE courtroom ADD UNIQUE USING INDEX ctr_chr_crn_unq;

--,UNIQUE(cas_id,ctr_id,c_hearing_date)
CREATE UNIQUE INDEX hea_cas_ctr_hd_unq ON hearing( cas_id, ctr_id,hearing_date) TABLESPACE darts_indexes;
ALTER TABLE hearing ADD UNIQUE USING INDEX hea_cas_ctr_hd_unq;

--,UNIQUE(cth_id, case_number)
CREATE UNIQUE INDEX cas_case_number_cth_id_unq ON court_case(case_number,cth_id) TABLESPACE darts_indexes;
ALTER TABLE court_case ADD UNIQUE USING INDEX cas_case_number_cth_id_unq;

GRANT SELECT,INSERT,UPDATE,DELETE ON annotation TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON audit TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON audit_activity TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON automated_task TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_judge_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_retention_event TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON court_case TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON courthouse TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON courthouse_region_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON courtroom TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON daily_list TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON defence TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON defendant TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON device_register TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON event TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON event_handler TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON external_location_type TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON external_object_directory TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON external_service_auth_token TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing_event_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing_media_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON judge TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON media TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON media_request TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON notification TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON object_directory_status TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON prosecutor TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON region TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON report TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_policy TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_comment TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_status TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_type TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_urgency TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_workflow TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transient_object_directory TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON user_account TO darts_user;

GRANT SELECT,UPDATE ON  ann_seq TO darts_user;
GRANT SELECT,UPDATE ON  aua_seq TO darts_user;
GRANT SELECT,UPDATE ON  aud_seq TO darts_user;
GRANT SELECT,UPDATE ON  aut_seq TO darts_user;
GRANT SELECT,UPDATE ON  car_seq TO darts_user;
GRANT SELECT,UPDATE ON  cas_seq TO darts_user;
GRANT SELECT,UPDATE ON  cra_seq TO darts_user;
GRANT SELECT,UPDATE ON  cre_seq TO darts_user;
GRANT SELECT,UPDATE ON  cth_seq TO darts_user;
GRANT SELECT,UPDATE ON  ctr_seq TO darts_user;
GRANT SELECT,UPDATE ON  dal_seq TO darts_user;
GRANT SELECT,UPDATE ON  dfc_seq TO darts_user;
GRANT SELECT,UPDATE ON  dfd_seq TO darts_user;
GRANT SELECT,UPDATE ON  der_seq TO darts_user;
GRANT SELECT,UPDATE ON  elt_seq TO darts_user;
GRANT SELECT,UPDATE ON  eod_seq TO darts_user;
GRANT SELECT,UPDATE ON  esa_seq TO darts_user;
GRANT SELECT,UPDATE ON  eve_seq TO darts_user;
GRANT SELECT,UPDATE ON  evh_seq TO darts_user;
GRANT SELECT,UPDATE ON  hea_seq TO darts_user;
GRANT SELECT,UPDATE ON  jud_seq TO darts_user;
GRANT SELECT,UPDATE ON  med_seq TO darts_user;
GRANT SELECT,UPDATE ON  mer_seq TO darts_user;
GRANT SELECT,UPDATE ON  not_seq TO darts_user;
GRANT SELECT,UPDATE ON  ods_seq TO darts_user;
GRANT SELECT,UPDATE ON  prn_seq TO darts_user;
GRANT SELECT,UPDATE ON  reg_seq TO darts_user;
GRANT SELECT,UPDATE ON  rep_seq TO darts_user;
GRANT SELECT,UPDATE ON  rtp_seq TO darts_user;
GRANT SELECT,UPDATE ON  tod_seq TO darts_user;
GRANT SELECT,UPDATE ON  tra_seq TO darts_user;
GRANT SELECT,UPDATE ON  trc_seq TO darts_user;
GRANT SELECT,UPDATE ON  trs_seq TO darts_user;
GRANT SELECT,UPDATE ON  trt_seq TO darts_user;
GRANT SELECT,UPDATE ON  tru_seq TO darts_user;
GRANT SELECT,UPDATE ON  trw_seq TO darts_user;
GRANT SELECT,UPDATE ON  usr_seq TO darts_user;

GRANT USAGE ON SCHEMA DARTS TO darts_user;

SET ROLE DARTS_USER;
SET SEARCH_PATH TO darts;








