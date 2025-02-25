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
--v48 remove grant on cra_seq, removed in v47
--    remove transcription.trs_id and FK from transcription_status
--    remove transcription-workflow.workflow_stage, and replace with transcription_status.trs_id
--    add FK from transcription_status to transcription_workflow
--    add Not Null to CAS.case_number, CAS.case_closed, CAS.interpreter_used
--    add NN to DAL.cth_id, DAL.job_status
--    add default of "DAR" to DER.device_type
--    add NN to HEA.hearing_is_actual, HEA.hearing_date
--    add NN to MED.channel and MED.total_channels, MED.start_ts, MED.end_ts
--    remove transcription.current_state_ts to be inferred from transcription_workflow
--v49 remove transcription.company, no legacy data to migrated and a transcription co linked to a CTH can be derived elsewhere
--    add transcription.trs_id, this is a system managed attribute, to be populated and maintained by
--    a trigger on transcription_workflow only
--    add transcription_workflow.workflow_actor & workflow_ts
--    amend user_account.user_name to NN
--    remove sequences trtseq, truseq, trsseq PK values must match values from legacy
--    add created* & last_modified* to transcription_status
--    add tablespace clause to transcription_status & transcription_type
--    add trigger and associated function to transcription_workflow
--v50 rename device_register to node_register, and column device_type to node_type
--    remove last_modifed* from COURTROOM, EVENT_HANDLER, NODE_REGISTER
--    remove created* & last_modified* from EXTERNAL_LOCATION_TYPE, OBJECT_DIRECTORY_STATUS, REGION
--    remove created* & last_modified* from TRANSCRIPTION_STATUS, TRANSCRIPTION_TYPE, TRANSCRIPTION_URGENCY, TRANSCRIPTION_WORKFLOW
--    remove workflow_comment from transcription_workflow, instead add trw_id to transcription_comment as foreign key to transcription_workflow
--    add table transcription_document including foreign key to transcription
--    replace column tra_id in external_object_directory with trd_id instead, with same change to FK
--v51 add table annotation_document, its PK, Sequence and FK to annotation
--    amend external_object_directory to refer to ado_id instead on ann_id
--    add hea_id to annotation and FK to hearing
--    adding FKs on ado_id, trd_id on external_object_directory
--    add is_reporting_restriction to event_handler table
--v52 removed: CREATE SEQUENCE der_seq
--    added: CREATE SEQUENCE nod_seq
--    removed comment: ALTER TABLE node_register...last_modified*
--    added: -- node_register               NOD
--    removed: -- device_register             DER
--    removed: GRANT SELECT,UPDATE ON  der_seq TO darts_user;
--    added: GRANT SELECT,UPDATE ON  nod_seq TO darts_user;
--    updated annotation_document - all columns NOT NULL
--    updated transcription_document - all columns NOT NULL
--v53 amend transcription.hearing_date from TIMESTAMP to DATE, to be consistent with hearing.hearing_date
--    add transcription.is_manual BOOLEAN 
--    add media_file, media_format, media_type (as FK to new table), file_size, checksum to media
--    add media_type table, primary key and fk from media
--    add is_system_user and account_guid to user_account
--    add display_name to courthouse
--v54 add checksum to annotation_document and transcription_document
--    add display_state to transcription_type and transcription_urgency
--    add display_name to transcription_status
--v55 remove retain_until_ts from court_case
--    add retention_applies_from to court_case
--    add end_of_sentence_ts to court_case
--    remove all retention related artefacts, to be included in separate file
--    including case_retention, case_retention_event, retention_policy
--    remove NN constraint in user_account
--    add current_owner to annotation, with FK to user_account
--    amend media.file_size to bigint
--    remove media_type table, and replace by media.media_type column
--v56 add hide_request_from_requestor field on transcription
--v57 add output_filesize to media_request
--    add priority_order to transcription_urgency
--    add message_id to daily_list
--    add retain_until_ts to court_case
--    replace user_state with is_active on user_accout
--    add content_object_id to annotation_document, transcription_document & media
--v58 rename object_directory_status to object_record_status
--    add table transformed_media, to link media_request to transient_object_directory ( as a media request can give rise to >1 pieces of media
--    some columns from media_request pushed down to transformed_media, where pertain to the media rather than the Request
--    add boolean is_hidden to annotation_document, transcription_document & media
--    add media_status column to media, to include statuses of "fully linked", "marked for deletion", "deleted"
--v59 add start_ts and end_ts to transformed_media
--    add user_full_name to user_account
--    remove foreign key from daily_list to courthouse, replace daily_list_cth_id text of courthouse 
--    introduce case_overflow table for additional, legacy only retention attributes for a case
--    remove retention_applied_from_ts and end_of_sentence_ts from court_case to case_overflow
--    remove retain_until_ts from court_case
--    add audio_folder_object_id to case_overflow
--    i_retain_until_ts to case_overflow to be populated from dm_sysobject_s of audio_folder
--v60 add associative entity hearing_annotation_ae, removing links from annotation to cas_id, hea_id and crt_id
--    add foreign keys from new table to hearing and annotation
--    add is_deleted to annotation
--    add is_standard_policy and is_permanent_policy to case_overflow
--    add manifest_file and event_date_ts to external_object_directory
--    add case_document table, new PK and sequence, FK to case and FK on external_object_directory to it
--v61 add associative entities to join case to transcription and hearing to transcription
--    remove hea_id and cas_id from transcription and associated FKs
--    change evh_id on event to not null
--    add verification_attempts to external_object_directory
--    change audit.cas_id to nullable, as not all modernised auditable events are case related
--    remove audit.application_server
--v62 change courhouse.courthouse_code to be nullable and remove unique clause
--    change media.ctr_id & event.ctr_id to be not null
--    add chronicle_id, antecedant_id to event, media & transcription
--    add error_code and is_response_cleaned to external_object_directory
--    add clip_id to media, annotation_document, transcription_document
--    amend event_ts to not null on event
--v63 add PK constraints to case_transcription_ae & hearing_transcription_ae
--    add courthouse_code to daily_list
--    amend transcription_commment.tra_id to be not null
--v64 add retain_until_ts to annotation_document, case_document, media, transcription_document
--    add last_modified_by/ts to annotation_document, case_document, transcription_document
--    add is_deleted to court_case, media, transcription
--    add update_retention to external_object_directory
--    add is_retention_updated to court_case
--    amend user_full_name and user_name on user_account to nullable
--    amend checksum on annotation_document, case_document, transcription_document to nullable
--    add retention_retries to court_case
--    amend retain_until_ts on annotation_document, case_document, media, transcription_document to nullable
--    add marked_for_manual_deletion to annotation_document, case_document, media, transcription_document
--    add table object_hidden_reason 
--    add fk and column ohr_id to annotation_document, case_document, media, transcription_document
--    amend is_hidden to default to false on annotation_document, case_document, media, transcription_document
--    amend is_deleted to default to false on annotation, court_case, media, transcription
--    add osr_uuid on external_object_directory
--    amend external_location on external_object_directory to nullable
--    add enhanced_auditing to audit table
--v65 add table audit_heritage
--v66 amend uploaded_by/ts to created_by/ts on case_document 
--    add hidden_by/ts to annotation_document, case_document, media, transcription_document (x v68)
--    add deleted_by/ts to annotation, court_case, media, transcription
--    add marked_for_manual_del_by/ts to annotation_document, case_document, media, transcription_document (x v68)
--    add FK on case_document for created_by and last_modified_by
--    add FK on hidden_by to annotation_document, case_document, media, transcription_document (x v68)
--    add FK on marked_for_manual_del_by to annotation_document, case_document, media, transcription_document (x v68)
--    add FK on deleted_by to annotation, court_case, media, transcription
--    add FK on created_by, last_modified_by on transformed_media
--v67 add 2 tables to support migration media_linked_legacy_case, event_linked_legacy_case
--v68 remove version_label from court_case,daily_list & report
--    remove enhanced_auditing from audit 
--    change name event_linked_legacy_case and media_linked_legacy_case to remove _legacy_
--    add checked_ts, corrected_ts to case_overflow
--    add confidence_level, confidence_reason to case_overflow
--    add batch_size to automated_task
--    add c_closed_pre_live & c_case_closed_date_pre_live to case_overflow
--    add is_data_anonymised, data_anonymised_by, data_anonymised_ts on court_case all nullable except court_case
--    add table object_admin_action, its PK and sequence
--    remove ohr_id from annotation_document, case_document, media, transcription_document
--    remove hidden_by and hidden_ts from annotation_document, case_document, media, transcription_document
--    remove marked_for_manual_del[etion, _by, _ts] from annotation_document, case_document, media, transcription_document
--    remove 3 FKs from annotation_document ( ohr_id, hidden_by, marked_for_manual_del_by )
--    remove 3 FKs from case_document ( ohr_id, hidden_by, marked_for_manual_del_by )
--    remove 3 FKs from media ( ohr_id, hidden_by, marked_for_manual_del_by )
--    remove 3 FKs from transcription_document ( ohr_id, hidden_by, marked_for_manual_del_by )
--    add 3 FKs to object_admin_action ( ohr_id, hidden_by, marked_for_manual_del_by )
--    add 4 FKs to object_admin_action ( ado_id, cad_id, med_id, trd_id )
--    add object_retrieval_queue, its PK and sequence
--v69 change 2 column names on the event_linked_case and media_linked_case to match modernised format
--    add cas_id to event_linked_case and media_linked_case
--    remove case_number from event and media
--    remove event_name from event, as evh_id is mandatory event_name should be derived from event_handler
--v70 add default to is_data_anonymised on court_case
--    make courthouse and case_number nullable on event_linked_case and media_linked_case
--    amend datatypes of courthouse and case_number on both tables, to match modernised
--    re-order list of optional FK columns in external_object_directory
--    add multi-column constraint on same
--    add similar multi-column constraints to event_linked_case, media_linked_case, object_admin_action, object_retrieval_queue
--    add 3 unique constraints, one defence, defendant and prosecutor to avoid duplicate names on a case
--    add case_object_name to case_overflow to support migration from dm_sysobject.object_name
--    add transcription_object_name to transcription, ditto
--    move confidence* columns from case_overflow to court_case and prefix with ret
--    replace requestor string to requested_by on transcription with fk to user_account
--    add event_status and is_current to event
--    add case_type, upload_priority to court_case
--    remove judge_hearing_date from hearing
--    remove reference_id from media
--    add is_migrated to transcription_comment
--    add default to court_case.is_data_anonymised
--v71 amend ret_conf_level to boolean from integer on court_case
--    add ret_conf_updated_ts to court_case
--    add the 3 ret_conf_* columns to annotation_document,case_document,media,transcription_document
--    add boolean is_current_migrated to media and transcription
--    amend event_id on event to bigint, current largest is 178million, int has capacity to 2 billion
--v71.1 amend is_current_migrated to is_current on media and transcription and nullable !
--    remove ret_conf_updated_ts from annotation_document,case_document,media,transcription_document
--    amend ret_conf_level to integer
--    amend ret_conf_level to ret_conf_score
--    revert  event_id to int
--    add content_object_id, clip_id, external_location to daily_list
--    add elt_id to daily_list
--    add is_deleted, deleted_by & deleted_ts to annotation_document, case_document, transcription_document
--    add is_data_anonymised to event and transcription_comment
--    add data_ingestion_ts to external_object_directory
--v71.2 add missing FK constraints to
--    annotation_document[deleted_by, uploaded_by,last_modified_by]
--    case_document[deleted_by]
--    court_case[data_anonymised_by]
--    object_retrieval_queue[created_by, last_modified_by]
--    transcription_document[delted_by, uploaded_by, last_modified_by]
--    amend courthouse sequence to nocache
--v71.3 amend datatype on eod.osr_uuid from char to int
--    nod_seq, ors_seq, trm_seq nocache
--    automated_task.task_enabled default true
--    court_case.is_retention_updated default false
--    event.is_current default true
--    transformed_media start_ts, end_ts to not null
--    user account is_system_user default to false
--    add unique index on user_account, upper(user_email_address)
--v71.4 amend all tablespaces to pg_default
-- make media_status nullable
--v71.5 add extension pg_trgm for trigram index support
--v72 add tables arm_rpo_state, arm_rpo_status, arm_rpo_execution_detail, arm_automated_task
--    amend media_linked_case, with 3 new columns (source,created_ts/by)
--    amend court_case, with 2 new columns (case_object_name, folder_path)
--    amend media, event, transcription with 1 new column, folder_path
--    add table data_anonymisation
--    add 2 columns to table external_object_directory
--v72.1 add 2 missing FKs on data_anonymisation
--    add missing FK on daily_list
--    add 2 FKs on event_linked_case
--    add 2 FKs on media_linked_case
--    add 2 FKs on object_retrieval_queue
--v72.2 reintroduce user_login_name,user_os_name,user_login_domain,user_global_unique_id,user_ldap_dn
--    to user_account
--    add subcontent_object_id and subcontent_position to annotation_document,
--    daily_list,media,transcription_document
--    add courthouse_object_id and folder_path to courthouse
--    reinstate numeric user_state to user_account
--    amend user_account.user_full_name to not null
--    add table transcription_linked_case, as per event_linked_case
--    add extobjdir_process_detail as 1:1 with external_object_directory ( c.f.case_overflow )
--    remove user_name from user_account
--v72.3 add rpt_id to case_overflow
--    add numerous columns to extobjdir_process_detail
--    move case_overflow to retention script
--    add c_current_state and r_current_state to transcription
--v72.4 add FKs to extobjdir_process_detail
--    add polling_created_ts to arm_rpo_execution_detail
--    add data_ticket to media, transcription_document, daily_list,annotation_document, object_retrieval_queue
--    reinstate user_name to user_account
--    remove folder_path from event,media & transcription
--    add production_name to arm_rpo_execution_detail
--    add is_dets to external_object_directory
--v72.5 add storage_id to media, transcription_document, daily_list,annotation_document, object_retrieval_queue

-- List of Table Aliases
-- annotation                  ANN
-- annotation_document         ADO
-- arm_automated_task          AAT
-- arm_rpo_execution_detail    ARD
-- arm_rpo_state               ARE
-- arm_rpo_status              ARU 
-- audit                       AUD
-- audit_activity              AUA
-- audit_heritage              AUH
-- automated_task              AUT
-- case_document               CAD
-- case_judge_ae               CAJ
-- case_transcription_ae       CAT
-- case_overflow               CAO
-- court_case                  CAS
-- courthouse                  CTH
-- courthouse_region_ae        CRA
-- courtroom                   CTR
-- data_anonymisation          DAN
-- daily_list                  DAL
-- defence                     DFC
-- defendant                   DFD
-- event                       EVE
-- event_handler               EVH
-- event_linked_case           ELC
-- external_location_type      ELT
-- external_object_directory   EOD
-- extobjdir_process_detail    EPD
-- external_service_auth_token ESA
-- hearing                     HEA
-- hearing_annotation_ae       HAA
-- hearing_event_ae            HEE
-- hearing_media_ae            HEM
-- hearing_judge_ae            HEJ
-- hearing_transcription_ae    HET
-- judge                       JUD
-- media                       MED
-- media_linked_case           MLC
-- media_request               MER
-- node_register               NOD
-- notification                NOT
-- object_admin_action         OAA
-- object_hidden_reason        OHR
-- object_record_status        ORS
-- object_retrieval_queue      ORQ
-- prosecutor                  PRN
-- region                      REG
-- report                      REP
-- transcription               TRA
-- transcription_comment       TRC
-- transcription_document      TRD
-- transcription_linked_case   TLC
-- transcription_status        TRS
-- transcription_type          TRT
-- transcription_urgency       TRU
-- transcription_workflow      TRW
-- transformed_media           TRM
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

CREATE EXTENSION IF NOT EXISTS pg_trgm;

--CREATE TABLESPACE darts_tables  location 'E:/PostgreSQL/Tables';
--CREATE TABLESPACE darts_indexes location 'E:/PostgreSQL/Indexes';

--GRANT ALL ON TABLESPACE darts_tables TO darts_owner;
--GRANT ALL ON TABLESPACE darts_indexes TO darts_owner;

--CREATE TABLESPACE pg_default  location 'E:/PostgreSQL/DARTS';
--GRANT ALL ON TABLESPACE pg_default TO darts_owner;

SET ROLE DARTS_OWNER;

SET SEARCH_PATH TO darts;

CREATE TABLE annotation
(ann_id                      INTEGER                       NOT NULL
,annotation_text             CHARACTER VARYING
,annotation_ts               TIMESTAMP WITH TIME ZONE
,annotation_object_id        CHARACTER VARYING(16)
,version_label               CHARACTER VARYING(32)
,current_owner               INTEGER                       NOT NULL
,is_deleted                  BOOLEAN                       NOT NULL DEFAULT false
,deleted_by                  INTEGER
,deleted_ts                  TIMESTAMP WITH TIME ZONE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL          
) TABLESPACE pg_default;

COMMENT ON COLUMN annotation.ann_id
IS 'primary key of annotation';

COMMENT ON COLUMN annotation.annotation_object_id
IS 'internal Documentum primary key from moj_annotation_s';

COMMENT ON COLUMN annotation.annotation_text
IS 'directly sourced from moj_annotation_s.c_text';

COMMENT ON COLUMN annotation.annotation_ts
IS 'directly sourced from moj_annotation_s';

COMMENT ON COLUMN annotation.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_annotation';

CREATE TABLE annotation_document
(ado_id                      INTEGER                       NOT NULL
,ann_id                      INTEGER                       NOT NULL
,content_object_id           CHARACTER VARYING(16)                  -- legacy PK from dmr_content 
,subcontent_object_id        CHARACTER VARYING(16)
,subcontent_position         INTEGER
,clip_id                     CHARACTER VARYING(54)
,storage_id                  CHARACTER VARYING(16)
,data_ticket                 INTEGER
,file_name                   CHARACTER VARYING             NOT NULL
,file_type                   CHARACTER VARYING             NOT NULL
,file_size                   INTEGER                       NOT NULL
,checksum                    CHARACTER VARYING
,is_deleted                  BOOLEAN                       NOT NULL DEFAULT false
,deleted_by                  INTEGER
,deleted_ts                  TIMESTAMP WITH TIME ZONE             
,is_hidden                   BOOLEAN                       NOT NULL DEFAULT false
,retain_until_ts             TIMESTAMP WITH TIME ZONE 
,ret_conf_score              INTEGER
,ret_conf_reason             CHARACTER VARYING
,uploaded_by                 INTEGER                       NOT NULL
,uploaded_ts                 TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL 
) TABLESPACE pg_default;

COMMENT ON COLUMN annotation_document.ado_id
IS 'primary key of annotation_document'; 

COMMENT ON COLUMN annotation_document.ann_id
IS 'foreign key from annotation'; 

CREATE TABLE arm_automated_task
(aat_id                      INTEGER                      NOT NULL
,aut_id                      INTEGER                      NOT NULL
,rpo_csv_start_hour          INTEGER
,rpo_csv_end_hour            INTEGER
,arm_replay_start_ts         TIMESTAMP WITH TIME ZONE
,arm_replay_end_ts           TIMESTAMP WITH TIME ZONE
,arm_attribute_type          CHARACTER VARYING
) TABLESPACE pg_default;

COMMENT ON COLUMN arm_automated_task.aat_id
IS 'primary key of arm_automated_task'; 

COMMENT ON COLUMN arm_automated_task.aut_id
IS 'foreign key to automated_task'; 

CREATE TABLE arm_rpo_execution_detail
(ard_id                      INTEGER                      NOT NULL
,are_id                      INTEGER                      
,aru_id                      INTEGER                      
,matter_id                   CHARACTER VARYING
,index_id                    CHARACTER VARYING
,entitlement_id              CHARACTER VARYING
,storage_account_id          CHARACTER VARYING
,search_id                   CHARACTER VARYING
,production_id               CHARACTER VARYING
,sorting_field               CHARACTER VARYING
,search_item_count           INTEGER
,polling_created_ts          TIMESTAMP WITH TIME ZONE
,production_name             CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN arm_rpo_execution_detail.ard_id
IS 'primary key of arm_rpo_execution_detail';

COMMENT ON COLUMN arm_rpo_execution_detail.are_id
IS 'foreign key to arm_rpo_state';

COMMENT ON COLUMN arm_rpo_execution_detail.aru_id
IS 'foreign key to arm_rpo_status';

CREATE TABLE arm_rpo_state
(are_id                       INTEGER                      NOT NULL
,are_description              CHARACTER VARYING            NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN arm_rpo_state.are_id
IS 'primary key of arm_rpo_state';

CREATE TABLE arm_rpo_status
(aru_id                       INTEGER                      NOT NULL
,aru_description              CHARACTER VARYING            NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN arm_rpo_status.aru_id
IS 'primary key of arm_rpo_status';

CREATE TABLE audit
(aud_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       
,aua_id                      INTEGER                       NOT NULL
,usr_id                      INTEGER                       NOT NULL
,additional_data             CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

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

) TABLESPACE pg_default;

COMMENT ON COLUMN audit.aua_id
IS 'primary key of audit_activity';

CREATE TABLE audit_heritage
(R_OBJECT_ID                  CHARACTER VARYING      
,EVENT_NAME                   CHARACTER VARYING      
,EVENT_SOURCE                 CHARACTER VARYING      
,R_GEN_SOURCE                 INTEGER                 
,USER_NAME                    CHARACTER VARYING
,AUDITED_OBJ_ID               CHARACTER VARYING
,TIME_STAMP                   TIMESTAMP WITH TIME ZONE
,STRING_1                     CHARACTER VARYING
,STRING_2                     CHARACTER VARYING
,STRING_3                     CHARACTER VARYING
,STRING_4                     CHARACTER VARYING
,STRING_5                     CHARACTER VARYING
,ID_1                         CHARACTER VARYING
,ID_2                         CHARACTER VARYING
,ID_3                         CHARACTER VARYING
,ID_4                         CHARACTER VARYING
,ID_5                         CHARACTER VARYING
,CHRONICLE_ID                 CHARACTER VARYING
,OBJECT_NAME                  CHARACTER VARYING
,VERSION_LABEL                CHARACTER VARYING
,OBJECT_TYPE                  CHARACTER VARYING
,EVENT_DESCRIPTION            CHARACTER VARYING
,POLICY_ID                    CHARACTER VARYING
,CURRENT_STATE                CHARACTER VARYING
,WORKFLOW_ID                  CHARACTER VARYING
,SESSION_ID                   CHARACTER VARYING
,USER_ID                      CHARACTER VARYING
,OWNER_NAME                   CHARACTER VARYING
,ACL_NAME                     CHARACTER VARYING
,ACL_DOMAIN                   CHARACTER VARYING
,APPLICATION_CODE             CHARACTER VARYING
,CONTROLLING_APP              CHARACTER VARYING
,ATTRIBUTE_LIST               CHARACTER VARYING
,ATTRIBUTE_LIST_ID            CHARACTER VARYING
,AUDIT_SIGNATURE              CHARACTER VARYING
,AUDIT_VERSION                INTEGER
,HOST_NAME                    CHARACTER VARYING
,TIME_STAMP_UTC               TIMESTAMP WITH TIME ZONE
,I_AUDITED_OBJ_CLASS          INTEGER
,REGISTRY_ID                  CHARACTER VARYING
,I_IS_ARCHIVED                INTEGER
,AUDITED_OBJ_VSTAMP           INTEGER
,ATTRIBUTE_LIST_OLD           CHARACTER VARYING
,I_IS_REPLICA                 INTEGER
,I_VSTAMP                     INTEGER
,ATTRIBUTE_LIST_ASPECT_ID     CHARACTER VARYING
,R_OBJECT_SEQUENCE            INTEGER
 ) TABLESPACE pg_default;


CREATE TABLE automated_task
(aut_id                      INTEGER                       NOT NULL
,task_name                   CHARACTER VARYING             NOT NULL
,task_description            CHARACTER VARYING             NOT NULL
,cron_expression             CHARACTER VARYING             NOT NULL
,cron_editable               BOOLEAN                       NOT NULL
,task_enabled                BOOLEAN                       NOT NULL DEFAULT true
,batch_size                  INTEGER 
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN automated_task.aut_id
IS 'primary key of automated_task';

CREATE TABLE case_document 
(cad_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,file_name                   CHARACTER VARYING             NOT NULL
,file_type                   CHARACTER VARYING             NOT NULL
,file_size                   INTEGER                       NOT NULL
,checksum                    CHARACTER VARYING   
,is_deleted                  BOOLEAN                       NOT NULL DEFAULT false
,deleted_by                  INTEGER
,deleted_ts                  TIMESTAMP WITH TIME ZONE                  
,is_hidden                   BOOLEAN                       NOT NULL DEFAULT false
,retain_until_ts             TIMESTAMP WITH TIME ZONE  
,ret_conf_score              INTEGER
,ret_conf_reason             CHARACTER VARYING
,created_by                  INTEGER                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL 
) TABLESPACE pg_default;


CREATE TABLE case_judge_ae
(cas_id                      INTEGER                       NOT NULL
,jud_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN case_judge_ae.cas_id
IS 'foreign key from case, part of composite natural key and PK';

COMMENT ON COLUMN case_judge_ae.jud_id
IS 'foreign key from judge, part of composite natural key and PK';

CREATE TABLE case_transcription_ae
(cas_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN case_transcription_ae.cas_id
IS 'foreign key from case, part of composite natural key and PK';

COMMENT ON COLUMN case_transcription_ae.tra_id
IS 'foreign key from transcription, part of composite natural key and PK';

CREATE TABLE court_case
(cas_id                      INTEGER                       NOT NULL
,cth_id                      INTEGER                       NOT NULL
,evh_id                      INTEGER                                 -- must map to one of the reporting restriction elements found on event_handler
,case_object_id              CHARACTER VARYING(16)
,case_object_name            CHARACTER VARYING(255)                  -- to accommodate dm_sysobject_s.object_name
,folder_path                 CHARACTER VARYING                       -- to accommodate dm_folder_r.r_folder_path
,case_number                 CHARACTER VARYING             NOT NULL  -- maps to c_case_id in legacy      
,case_type                   CHARACTER VARYING                       -- maps to c_type in legacy
,upload_priority             INTEGER                                 -- maps to c_priority in legacy              
,case_closed                 BOOLEAN                       NOT NULL
,interpreter_used            BOOLEAN                       NOT NULL
,case_closed_ts              TIMESTAMP WITH TIME ZONE
,is_retention_updated        BOOLEAN                       NOT NULL DEFAULT false -- flag to indicate retention has been updated
,retention_retries           INTEGER
,is_data_anonymised          BOOLEAN                       NOT NULL DEFAULT false
,data_anonymised_by          INTEGER
,data_anonymised_ts          TIMESTAMP WITH TIME ZONE
,ret_conf_score              INTEGER
,ret_conf_reason             CHARACTER VARYING
,ret_conf_updated_ts         TIMESTAMP WITH TIME ZONE
,is_deleted                  BOOLEAN                       NOT NULL DEFAULT false
,deleted_by                  INTEGER
,deleted_ts                  TIMESTAMP WITH TIME ZONE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

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

CREATE TABLE courthouse
(cth_id                      INTEGER                       NOT NULL
,courthouse_object_id        CHARACTER VARYING(16)
,folder_path                 CHARACTER VARYING
,courthouse_code             INTEGER                       
,courthouse_name             CHARACTER VARYING             NOT NULL          UNIQUE
,display_name                CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN courthouse.cth_id
IS 'primary key of courthouse';

COMMENT ON COLUMN courthouse.courthouse_code
IS 'corresponds to the c_crown_court_code found in daily lists';

COMMENT ON COLUMN courthouse.courthouse_name
IS 'directly sourced from moj_courthouse_s.c_id';

CREATE TABLE courthouse_region_ae
(cth_id                      INTEGER                       NOT NULL
,reg_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

CREATE TABLE courtroom
(ctr_id                      INTEGER                       NOT NULL
,cth_id                      INTEGER                       NOT NULL
,courtroom_name              CHARACTER VARYING             NOT NULL
--,UNIQUE(moj_cth_id,courtroom_name)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN courtroom.ctr_id
IS 'primary key of courtroom';

COMMENT ON COLUMN courtroom.cth_id
IS 'foreign key to courthouse';

CREATE TABLE data_anonymisation
(dan_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                                  -- only one of these must be populated, unenforced FK
,trc_id                      INTEGER                                  -- only one of these must be populated, unenforced FK
,is_manual_request           BOOLEAN
,requested_by                INTEGER                       NOT NULL
,requested_ts                TIMESTAMP WITH TIME ZONE      NOT NULL
,approved_by                 INTEGER                       
,approved_ts                 TIMESTAMP WITH TIME ZONE      
) TABLESPACE pg_default;

COMMENT ON COLUMN data_anonymisation.dan_id
IS 'primary key of data_anonymisation';

COMMENT ON COLUMN data_anonymisation.eve_id
IS 'foreign key to event, unenforced to support deletion of event without reference to this table';

COMMENT ON COLUMN data_anonymisation.trc_id
IS 'foreign key of transcription_comment, unenforce to support deletion of transcription_comment without reference to this table';

CREATE TABLE daily_list
(dal_id                      INTEGER                       NOT NULL
,courthouse_code             INTEGER                       
,listing_courthouse          CHARACTER VARYING             NOT NULL
,daily_list_object_id        CHARACTER VARYING(16)
,unique_id                   CHARACTER VARYING
--,c_crown_court_name        CHARACTER VARYING        -- removed, normalised to courthouses, but note that in legacy there is mismatch between moj_courthouse_s.c_id and moj_daily_list_s.c_crown_court_name to be resolved
,job_status                  CHARACTER VARYING             NOT NULL  -- one of "New","Partially Processed","Processed","Ignored","Invalid"
,published_ts                TIMESTAMP WITH TIME ZONE 
,start_dt                    DATE    
,end_dt                      DATE -- all values match c_start_date
,daily_list_id_s             CHARACTER VARYING        -- non unique integer in legacy
,daily_list_source           CHARACTER VARYING        -- one of CPP,XHB ( live also sees nulls and spaces)   
,daily_list_content_json     CHARACTER VARYING
,daily_list_content_xml      CHARACTER VARYING
,message_id                  CHARACTER VARYING 
,content_object_id           CHARACTER VARYING(16)
,subcontent_object_id        CHARACTER VARYING(16)
,subcontent_position         INTEGER
,clip_id                     CHARACTER VARYING
,storage_id                  CHARACTER VARYING(16)
,data_ticket                 INTEGER
,external_location           UUID
,elt_id                      INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN daily_list.dal_id
IS 'primary key of daily_list';

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

CREATE TABLE defence
(dfc_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,defence_name                CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

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
) TABLESPACE pg_default;

COMMENT ON COLUMN defendant.dfd_id 
IS 'primary key of defendant';

COMMENT ON COLUMN defendant.cas_id
IS 'foreign key from court_case';


CREATE TABLE event
(eve_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER                       NOT NULL
,evh_id                      INTEGER                       NOT NULL  --  based on the content of dm_sysobject.object_name 
,event_object_id             CHARACTER VARYING(16)                   -- legacy id of this event
,event_id                    INTEGER                       
,event_text                  CHARACTER VARYING
,event_ts                    TIMESTAMP WITH TIME ZONE      NOT NULL
,message_id                  CHARACTER VARYING
,is_log_entry                BOOLEAN                       NOT NULL  -- needs to be not null to ensure only 2 valid states
,event_status                INTEGER
,is_current                  BOOLEAN                       NOT NULL DEFAULT true
,is_data_anonymised          BOOLEAN                       NOT NULL DEFAULT false
,version_label               CHARACTER VARYING(32)
,chronicle_id                CHARACTER VARYING(16)                   -- legacy id of the 1.0 version of the event
,antecedent_id               CHARACTER VARYING(16)                   -- legacy id of the immediately  preceding event 
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

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
,is_reporting_restriction    BOOLEAN                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
) TABLESPACE pg_default;

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

COMMENT ON COLUMN event_handler.handler
IS 'to indicate if the event pertains to reporting restrictions, both application of, and lifting, in order to provide timeline of RR as applied to a case';

CREATE TABLE event_linked_case
(elc_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       NOT NULL     -- unenforced FK to event
,cas_id                      INTEGER                                    -- unenforced and optional FK
,courthouse_name             CHARACTER VARYING         
,case_number                 CHARACTER VARYING         
) TABLESPACE pg_default;

COMMENT ON TABLE event_linked_case
IS 'content is to be populated via migration';

CREATE TABLE external_object_directory
(eod_id                      INTEGER                       NOT NULL
,ado_id                      INTEGER                                 -- FK to annotation_document
,cad_id                      INTEGER                                 -- FK to case_document
,med_id                      INTEGER                                 -- FK to media
,trd_id                      INTEGER                                 -- FK to transcription_document
,ors_id                      INTEGER                       NOT NULL  -- FK to object_record_status
,elt_id                      INTEGER                       NOT NULL  -- FK to external_location_type 
,osr_uuid                    BIGINT                                  -- logical FK to object_state_record
-- additional optional FKs to other relevant internal objects would require columns here
,external_location           UUID                                    -- for use where address of Ext Obj requires 1 field
,external_file_id            CHARACTER VARYING                       -- for use where address of Ext Obj requires 2 fields
,external_record_id          CHARACTER VARYING                       -- for use where address of Ext Obj requires 2 fields
,checksum                    CHARACTER VARYING                       
,transfer_attempts           INTEGER
,manifest_file               CHARACTER VARYING
,event_date_ts               TIMESTAMP WITH TIME ZONE                -- date upon which the retention date in ARM is derived
,data_ingestion_ts           TIMESTAMP WITH TIME ZONE
,verification_attempts       INTEGER
,error_code                  CHARACTER VARYING
,is_response_cleaned         BOOLEAN                       NOT NULL DEFAULT false
,update_retention            BOOLEAN                       NOT NULL  -- flag to indicate a change that requires retention to be updated has occurred
,input_upload_processed_ts   TIMESTAMP WITH TIME ZONE
,force_response_cleanup      BOOLEAN
,is_dets                     BOOLEAN                       NOT NULL DEFAULT false
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN external_object_directory.eod_id
IS 'primary key of external_object_directory';

COMMENT ON COLUMN external_object_directory.elt_id
IS 'foreign key from external_location_type';

-- added two foreign key columns, but there will be as many FKs as there are distinct objects with externally stored components

COMMENT ON COLUMN external_object_directory.med_id
IS 'foreign key from media';

COMMENT ON COLUMN external_object_directory.trd_id
IS 'foreign key from transcription_document';

COMMENT ON COLUMN external_object_directory.ado_id
IS 'foreign key from annotation_document';

CREATE TABLE extobjdir_process_detail
(epd_id                       INTEGER                       NOT NULL
,eod_id                       INTEGER                       NOT NULL          UNIQUE
,event_date_ts                TIMESTAMP WITH TIME ZONE
,update_retention             BOOLEAN                       NOT NULL
,input_upload_filename        CHARACTER VARYING
,create_record_filename       CHARACTER VARYING
,create_record_processed_ts   TIMESTAMP WITH TIME ZONE
,upload_file_filename         CHARACTER VARYING
,upload_file_processed_ts     TIMESTAMP WITH TIME ZONE
,create_rec_inv_filename      CHARACTER VARYING
,create_rec_inv_processed_ts  TIMESTAMP WITH TIME ZONE
,upload_file_inv_filename     CHARACTER VARYING
,upload_file_inv_processed_ts TIMESTAMP WITH TIME ZONE 
,created_ts                   TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                   INTEGER                       NOT NULL
,last_modified_ts             TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by             INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON TABLE extobjdir_process_detail 
IS 'This table is an extension of external_object_directory.';

COMMENT ON COLUMN extobjdir_process_detail.epd_id
IS 'primary key of extobjdir_process_detail, needed to support Hibernate. Logically eod_id is the PK';

COMMENT ON COLUMN extobjdir_process_detail.eod_id
IS 'foreign key from external_object_directory';

CREATE TABLE external_location_type
(elt_id                      INTEGER                       NOT NULL
,elt_description             CHARACTER VARYING
) TABLESPACE pg_default;

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
) TABLESPACE pg_default;

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
,hearing_date                DATE                          NOT NULL   -- to record only DATE component of hearings, both scheduled and actual
,scheduled_start_time        TIME                                     -- to record only TIME component of hearings, while they are scheduled only
,hearing_is_actual           BOOLEAN                       NOT NULL   -- TRUE for actual hearings, FALSE for scheduled hearings
--,UNIQUE(moj_cas_id,moj_ctr,c_hearing_date)
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN hearing.hea_id
IS 'primary key of hearing';

COMMENT ON COLUMN hearing.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN hearing.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN hearing.hearing_date
IS 'directly sourced from moj_case_r';

CREATE TABLE hearing_annotation_ae
(hea_id                      INTEGER                       NOT NULL
,ann_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN hearing_annotation_ae.hea_id
IS 'foreign key from hearing, part of composite natural key and PK';

COMMENT ON COLUMN hearing_annotation_ae.ann_id
IS 'foreign key from annotation, part of composite natural key and PK';

CREATE TABLE hearing_event_ae
(hea_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN hearing_event_ae.hea_id
IS 'foreign key from hearing, part of composite natural key and PK';

COMMENT ON COLUMN hearing_event_ae.eve_id
IS 'foreign key from event, part of composite natural key and PK';

CREATE TABLE hearing_judge_ae
(hea_id                      INTEGER                       NOT NULL
,jud_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN hearing_judge_ae.hea_id
IS 'foreign key from case, part of composite natural key and PK';

COMMENT ON COLUMN hearing_judge_ae.jud_id
IS 'foreign key from judge, part of composite natural key and PK';

CREATE TABLE hearing_media_ae
(hea_id                      INTEGER                       NOT NULL
,med_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN hearing_media_ae.hea_id
IS 'foreign key from hearing, part of composite natural key and PK';

COMMENT ON COLUMN hearing_media_ae.med_id
IS 'foreign key from media, part of composite natural key and PK';

CREATE TABLE hearing_transcription_ae
(hea_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN hearing_transcription_ae.hea_id
IS 'foreign key from hearing, part of composite natural key and PK';

COMMENT ON COLUMN hearing_transcription_ae.tra_id
IS 'foreign key from transcription, part of composite natural key and PK';

CREATE TABLE judge
(jud_id                      INTEGER                       NOT NULL
,judge_name                  CHARACTER VARYING             NOT NULL          UNIQUE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN judge.jud_id 
IS 'primary key of judge';

CREATE TABLE media
(med_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER                       NOT NULL
,media_object_id             CHARACTER VARYING(16)                  -- legacy id of this media
,content_object_id           CHARACTER VARYING(16)                  -- legacy id of the content record associated with the external media
,subcontent_object_id        CHARACTER VARYING(16)
,subcontent_position         INTEGER
,clip_id                     CHARACTER VARYING(54)
,storage_id                  CHARACTER VARYING(16)
,data_ticket                 INTEGER
,channel                     INTEGER                       NOT NULL -- 1,2,3,4 or rarely 5
,total_channels              INTEGER                       NOT NULL --99.9% are "4" in legacy, occasionally 1,2,5 
,start_ts                    TIMESTAMP WITH TIME ZONE      NOT NULL
,end_ts                      TIMESTAMP WITH TIME ZONE      NOT NULL
,media_file                  CHARACTER VARYING             NOT NULL -- to accommodate dm_sysobject.object_name
,media_format                CHARACTER VARYING             NOT NULL
,media_type                  CHAR(1)                       NOT NULL DEFAULT 'A'
,file_size                   BIGINT                        NOT NULL
,checksum                    CHARACTER VARYING
,is_hidden                   BOOLEAN                       NOT NULL DEFAULT false
,is_deleted                  BOOLEAN                       NOT NULL DEFAULT false
,is_current                  BOOLEAN                       
,deleted_by                  INTEGER
,deleted_ts                  TIMESTAMP WITH TIME ZONE
,media_status                CHARACTER VARYING             
,version_label               CHARACTER VARYING(32)
,chronicle_id                CHARACTER VARYING(16)                   -- legacy id of the 1.0 version of the event
,antecedent_id               CHARACTER VARYING(16)                   -- legacy id of the immediately  preceding event 
,retain_until_ts             TIMESTAMP WITH TIME ZONE 
,ret_conf_score              INTEGER
,ret_conf_reason             CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

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

COMMENT ON COLUMN media.start_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN media.end_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN media.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_media';

CREATE TABLE media_linked_case
(mlc_id                      INTEGER                       NOT NULL
,med_id                      INTEGER                       NOT NULL     -- unenforced FK to media
,cas_id                      INTEGER                                    -- unenforced and optional FK
,courthouse_name             CHARACTER VARYING
,case_number                 CHARACTER VARYING
,source                      INTEGER                       DEFAULT 0    -- 1=AddAudio, 2=AudioLinkingByEvent, 3=AudioLinkingJob
,created_ts                  TIMESTAMP WITH TIME ZONE      
,created_by                  INTEGER                       
) TABLESPACE pg_default;

COMMENT ON TABLE media_linked_case
IS 'content is to be populated via migration';

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
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

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



CREATE TABLE node_register
(node_id                     INTEGER                       NOT NULL  --pk column breaks pattern used, is not nod_id
,ctr_id                      INTEGER                       NOT NULL
,node_type                   CHARACTER VARYING             NOT NULL DEFAULT 'DAR'
,hostname                    CHARACTER VARYING             NOT NULL
,ip_address                  CHARACTER VARYING             NOT NULL
,mac_address                 CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON TABLE node_register
IS 'corresponds to tbl_moj_node from legacy';

COMMENT ON COLUMN node_register.node_id
IS 'primary key of node_register';

COMMENT ON COLUMN node_register.ctr_id 
IS 'foreign key from moj_courtroom, legacy stored courthouse and courtroon un-normalised';

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
) TABLESPACE pg_default;

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

CREATE TABLE object_admin_action
(oaa_id                      INTEGER                       NOT NULL           
,ado_id                      INTEGER
,cad_id                      INTEGER
,med_id                      INTEGER
,trd_id                      INTEGER
,ohr_id                      INTEGER  
,hidden_by                   INTEGER
,hidden_ts                   TIMESTAMP WITH TIME ZONE
,marked_for_manual_deletion  BOOLEAN                       NOT NULL DEFAULT false
,marked_for_manual_del_by    INTEGER
,marked_for_manual_del_ts    TIMESTAMP WITH TIME ZONE
,ticket_reference            CHARACTER VARYING
,comments                    CHARACTER VARYING
) TABLESPACE pg_default;

CREATE TABLE object_hidden_reason
(ohr_id                      INTEGER                       NOT NULL
,ohr_reason                  CHARACTER VARYING             NOT NULL
,display_name                CHARACTER VARYING             NOT NULL
,display_state               BOOLEAN                       NOT NULL DEFAULT true
,display_order               INTEGER                       NOT NULL
,marked_for_deletion         BOOLEAN                       NOT NULL DEFAULT false
) TABLESPACE pg_default;

COMMENT ON TABLE object_hidden_reason
IS 'used to record acceptable reasons for data to be hidden in tables ADO,CAD, MED, TRD';

CREATE TABLE object_record_status
(ors_id                      INTEGER                       NOT NULL
,ors_description             CHARACTER VARYING
) TABLESPACE pg_default;

COMMENT ON TABLE object_record_status
IS 'used to record acceptable statuses found in [external/transient]_object_directory';

CREATE TABLE object_retrieval_queue
(orq_id                      INTEGER                       NOT NULL
,med_id                      INTEGER
,trd_id                      INTEGER
,parent_object_id            CHARACTER VARYING
,content_object_id           CHARACTER VARYING
,storage_id                  CHARACTER VARYING(16)
,clip_id                     CHARACTER VARYING
,data_ticket                 INTEGER
,acknowledged_ts             TIMESTAMP WITH TIME ZONE
,migrated_ts                 TIMESTAMP WITH TIME ZONE
,status                      CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

CREATE TABLE prosecutor
(prn_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,prosecutor_name             CHARACTER VARYING             NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN prosecutor.prn_id 
IS 'primary key of prosecutor';

COMMENT ON COLUMN prosecutor.cas_id
IS 'foreign key from court_case';

CREATE TABLE region
(reg_id                      INTEGER                       NOT NULL
,region_name                 CHARACTER VARYING             NOT NULL
) TABLESPACE pg_default;


CREATE TABLE report               
(rep_id                      INTEGER                       NOT NULL
,report_object_id            CHARACTER VARYING(16)
,name                        CHARACTER VARYING
,subject                     CHARACTER VARYING
,report_text                 CHARACTER VARYING
,query                       CHARACTER VARYING
,recipients                  CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

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

CREATE TABLE transcription
(tra_id                      INTEGER                       NOT NULL
,trt_id                      INTEGER                       NOT NULL
,ctr_id                      INTEGER                  
,tru_id                      INTEGER                                -- remains nullable, as nulls present in source data ( c_urgency)       
,trs_id                      INTEGER                                -- to be set according to trigger on transcription_workflow only
,transcription_object_id     CHARACTER VARYING(16)                  -- legacy pk from moj_transcription_s.r_object_id
,transcription_object_name   CHARACTER VARYING(255)                 -- to accommodate dm_sysobject_s.object_name
,requested_by                INTEGER                                -- 1055 distinct, from <forname><surname> to <AAANNA>
,hearing_date                DATE                                   -- 3k records have time component, but all times are 23:00,so effectively DATE only, will be absolete once moj_hea_id populated
,start_ts                    TIMESTAMP WITH TIME ZONE               -- both c_start and c_end have time components
,end_ts                      TIMESTAMP WITH TIME ZONE               -- we have 49k rows in legacy moj_transcription_s, 7k have c_end != c_start
,is_manual_transcription     BOOLEAN                       NOT NULL
,is_current                  BOOLEAN                       
,hide_request_from_requestor BOOLEAN                       NOT NULL 
,is_deleted                  BOOLEAN                       NOT NULL DEFAULT false
,deleted_by                  INTEGER
,deleted_ts                  TIMESTAMP WITH TIME ZONE
,version_label               CHARACTER VARYING(32)
,chronicle_id                CHARACTER VARYING(16)                   -- legacy id of the 1.0 version of the event
,antecedent_id               CHARACTER VARYING(16)                   -- legacy id of the immediately  preceding event 
,c_current_state             CHARACTER VARYING                       -- legacy field from moj_transcription
,r_current_state             INTEGER                                 -- legacy field from dm_sysobject for transcription type
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN transcription.tra_id
IS 'primary key of transcription';

COMMENT ON COLUMN transcription.trt_id
IS 'foreign key to transcription_type, sourced from moj_transcription_s.c_type';

COMMENT ON COLUMN transcription.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN transcription.tru_id
IS 'foreign key from transcription_urgency';

COMMENT ON COLUMN transcription.transcription_object_id
IS 'internal Documentum primary key from moj_transcription_s';
    
COMMENT ON COLUMN transcription.requested_by
IS 'foreign key from user_account, corresponding to moj_transcription_s.c_requestor';

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
,tra_id                      INTEGER                       NOT NULL
,trw_id                      INTEGER
,transcription_object_id     CHARACTER VARYING(16)         -- this is a placeholder for moj_transcription_s.r_object_id
,transcription_comment       CHARACTER VARYING
,comment_ts                  TIMESTAMP WITH TIME ZONE
,author                      INTEGER                       -- will need to be FK to user table
,is_migrated                 BOOLEAN                       NOT NULL default false
,is_data_anonymised          BOOLEAN                       NOT NULL DEFAULT false
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON COLUMN transcription_comment.trc_id
IS 'primary key of transcription_comment'; 

COMMENT ON COLUMN transcription_comment.tra_id
IS 'foreign key from transcription'; 

COMMENT ON COLUMN transcription_comment.trw_id
IS 'foreign key from transcription_workflow'; 

COMMENT ON COLUMN transcription_comment.transcription_object_id
IS 'internal Documentum primary key from moj_transcription_s'; 

COMMENT ON COLUMN transcription_comment.transcription_comment
IS 'directly sourced from moj_transcription_r';

COMMENT ON COLUMN transcription_comment.transcription_object_id
IS 'internal Documentum id from moj_transcription_s acting as foreign key';

CREATE TABLE transcription_document
(trd_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER                       NOT NULL
,content_object_id           CHARACTER VARYING(16)                  -- legacy PK from dmr_content object
,subcontent_object_id        CHARACTER VARYING(16)
,subcontent_position         INTEGER
,clip_id                     CHARACTER VARYING(54)
,storage_id                  CHARACTER VARYING(16)
,data_ticket                 INTEGER
,file_name                   CHARACTER VARYING             NOT NULL
,file_type                   CHARACTER VARYING             NOT NULL
,file_size                   INTEGER                       NOT NULL
,checksum                    CHARACTER VARYING 
,is_deleted                  BOOLEAN                       NOT NULL DEFAULT false
,deleted_by                  INTEGER
,deleted_ts                  TIMESTAMP WITH TIME ZONE                    
,is_hidden                   BOOLEAN                       NOT NULL DEFAULT false
,retain_until_ts             TIMESTAMP WITH TIME ZONE     
,ret_conf_score              INTEGER
,ret_conf_reason             CHARACTER VARYING
,uploaded_by                 INTEGER                       NOT NULL
,uploaded_ts                 TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL 
) TABLESPACE pg_default;

COMMENT ON COLUMN transcription_document.trd_id
IS 'primary key of transcription_document'; 

COMMENT ON COLUMN transcription_document.tra_id
IS 'foreign key from transcription'; 

CREATE TABLE transcription_linked_case
(tlc_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER                       NOT NULL     -- unenforced FK to transcription
,cas_id                      INTEGER                                    -- unenforced and optional FK
,courthouse_name             CHARACTER VARYING         
,case_number                 CHARACTER VARYING         
) TABLESPACE pg_default;

COMMENT ON COLUMN transcription_linked_case.tlc_id
IS 'primary key of transcription_linked_case'; 

COMMENT ON COLUMN transcription_linked_case.tra_id
IS 'foreign key from transcription'; 

COMMENT ON COLUMN transcription_linked_case.cas_id
IS 'foreign key from court_case, mandatory if either courthouse_name or case_number are null'; 

COMMENT ON COLUMN transcription_linked_case.courthouse_name
IS 'mandatory if cas_id is null'; 

COMMENT ON COLUMN transcription_linked_case.case_number
IS 'mandatory if cas_id is null'; 

CREATE TABLE transcription_status
(trs_id                      INTEGER                       NOT NULL
,status_type                 CHARACTER VARYING             NOT NULL
,display_name                CHARACTER VARYING             NOT NULL
)TABLESPACE pg_default; 

COMMENT ON TABLE transcription_status
IS 'standing data table';

COMMENT ON COLUMN transcription_status.trs_id
IS 'primary key of transcription_status';


CREATE TABLE transcription_type
(trt_id                      INTEGER                       NOT NULL
,description                 CHARACTER VARYING             NOT NULL
,display_state               BOOLEAN                       NOT NULL       
)TABLESPACE pg_default;

COMMENT ON TABLE transcription_type
IS 'standing data table, migrated from tbl_moj_transcription_type';

COMMENT ON COLUMN transcription_type.trt_id
IS 'primary key, but not sequence generated';


CREATE TABLE transcription_urgency
(tru_id                      INTEGER                       NOT NULL
,description                 CHARACTER VARYING             NOT NULL
,display_state               BOOLEAN                       NOT NULL
,priority_order              INTEGER                       NOT NULL
) TABLESPACE pg_default;

COMMENT ON TABLE transcription_urgency 
IS 'will be migrated from tbl_moj_urgency';

COMMENT ON COLUMN transcription_urgency.tru_id 
IS 'inherited from tbl_moj_urgency.urgency_id';

COMMENT ON COLUMN transcription_urgency.description
IS 'inherited from tbl_moj_urgency.description';

CREATE TABLE transcription_workflow
(trw_id                      INTEGER                       NOT NULL 
,tra_id                      INTEGER                       NOT NULL  -- FK to transcription 
,trs_id                      INTEGER                       NOT NULL  -- FK to transciption_status
,workflow_actor              INTEGER                       NOT NULL  -- FK to account_user
,workflow_ts                 TIMESTAMP WITH TIME ZONE      NOT NULL
--,workflow_comment            CHARACTER VARYING             
) TABLESPACE pg_default;

CREATE TABLE transformed_media
(trm_id                      INTEGER                       NOT NULL
,mer_id                      INTEGER                       NOT NULL  -- FK to media_request
,start_ts                    TIMESTAMP WITH TIME ZONE      NOT NULL
,end_ts                      TIMESTAMP WITH TIME ZONE      NOT NULL
,last_accessed_ts            TIMESTAMP WITH TIME ZONE
,expiry_ts                   TIMESTAMP WITH TIME ZONE
,output_filename             CHARACTER VARYING
,output_filesize             INTEGER
,output_format               CHARACTER VARYING
,checksum                    CHARACTER VARYING 
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL  
) TABLESPACE pg_default;

COMMENT ON TABLE transformed_media
IS 'to accommodate the possibility that a single media_request may be fulfilled as more than one resulting piece of media';

COMMENT ON COLUMN transformed_media.output_filename
IS 'filename of the requested media object, possibly migrated from moj_transformation_request_s';

COMMENT ON COLUMN transformed_media.output_format
IS 'format of the requested media object, possibly migrated from moj_transformation_s';

CREATE TABLE transient_object_directory
(tod_id                      INTEGER                       NOT NULL
,trm_id                      INTEGER                       NOT NULL  -- FK to transformed_media 
,ors_id                      INTEGER                       NOT NULL  -- FK to moj_object_record_status.moj_ors_id
,external_location           UUID                          NOT NULL
,checksum                    CHARACTER VARYING
,transfer_attempts           INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL                    
) TABLESPACE pg_default;


CREATE TABLE user_account
(usr_id                      INTEGER                       NOT NULL
,dm_user_s_object_id         CHARACTER VARYING(16)
,user_name                   CHARACTER VARYING                      -- to accommodate legacy data only
,user_os_name                CHARACTER VARYING            
,user_full_name              CHARACTER VARYING             NOT NULL            
,user_email_address          CHARACTER VARYING
,user_ldap_dn                CHARACTER VARYING
,user_global_unique_id       CHARACTER VARYING
,user_login_name             CHARACTER VARYING
,user_login_domain           CHARACTER VARYING
,user_state                  SMALLINT
,description                 CHARACTER VARYING
,is_active                   BOOLEAN                       NOT NULL
,last_login_ts               TIMESTAMP WITH TIME ZONE
,is_system_user              BOOLEAN                       NOT NULL DEFAULT false 
,account_guid                CHARACTER VARYING             
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL                    
) TABLESPACE pg_default;

COMMENT ON TABLE user_account 
IS 'migration columns all sourced directly from dm_user_s, but only for rows where r_is_group = 0';
COMMENT ON COLUMN user_account.usr_id
IS 'primary key of user_account';
COMMENT ON COLUMN user_account.dm_user_s_object_id
IS 'internal Documentum primary key from dm_user_s';
COMMENT ON COLUMN user_account.user_full_name
IS 'directly migrated from dm_user_s.user_name';


-- primary keys

CREATE UNIQUE INDEX annotation_pk ON annotation(ann_id) TABLESPACE pg_default;
ALTER TABLE annotation                 ADD PRIMARY KEY USING INDEX annotation_pk;

CREATE UNIQUE INDEX annotation_document_pk ON annotation_document(ado_id) TABLESPACE pg_default;
ALTER TABLE annotation_document        ADD PRIMARY KEY USING INDEX annotation_document_pk;

CREATE UNIQUE INDEX arm_automated_task_pk ON arm_automated_task(aat_id) TABLESPACE pg_default;
ALTER TABLE arm_automated_task   ADD PRIMARY KEY USING INDEX arm_automated_task_pk;

CREATE UNIQUE INDEX arm_rpo_execution_detail_pk ON arm_rpo_execution_detail(ard_id) TABLESPACE pg_default;
ALTER TABLE arm_rpo_execution_detail   ADD PRIMARY KEY USING INDEX arm_rpo_execution_detail_pk;

CREATE UNIQUE INDEX arm_rpo_state_pk ON arm_rpo_state(are_id) TABLESPACE pg_default;
ALTER TABLE arm_rpo_state              ADD PRIMARY KEY USING INDEX arm_rpo_state_pk;

CREATE UNIQUE INDEX arm_rpo_status_pk ON arm_rpo_status(aru_id) TABLESPACE pg_default;
ALTER TABLE arm_rpo_status             ADD PRIMARY KEY USING INDEX arm_rpo_status_pk;

CREATE UNIQUE INDEX audit_pk ON audit(aud_id) TABLESPACE pg_default;
ALTER TABLE audit                      ADD PRIMARY KEY USING INDEX audit_pk;

CREATE UNIQUE INDEX audit_activity_pk ON audit_activity(aua_id) TABLESPACE pg_default;
ALTER TABLE audit_activity             ADD PRIMARY KEY USING INDEX audit_activity_pk;

CREATE UNIQUE INDEX automated_task_pk ON automated_task(aut_id) TABLESPACE pg_default;
ALTER TABLE automated_task             ADD PRIMARY KEY USING INDEX automated_task_pk;

CREATE UNIQUE INDEX case_document_pk ON case_document(cad_id) TABLESPACE pg_default; 
ALTER TABLE case_document        ADD PRIMARY KEY USING INDEX case_document_pk;

CREATE UNIQUE INDEX case_judge_ae_pk ON case_judge_ae(cas_id,jud_id) TABLESPACE pg_default;
ALTER TABLE case_judge_ae        ADD PRIMARY KEY USING INDEX case_judge_ae_pk;

CREATE UNIQUE INDEX case_transcription_ae_pk ON case_transcription_ae(cas_id,tra_id) TABLESPACE pg_default;
ALTER TABLE case_transcription_ae        ADD PRIMARY KEY USING INDEX case_transcription_ae_pk;

CREATE UNIQUE INDEX court_case_pk ON court_case(cas_id) TABLESPACE pg_default; 
ALTER TABLE court_case              ADD PRIMARY KEY USING INDEX court_case_pk;

CREATE UNIQUE INDEX courthouse_pk ON courthouse(cth_id) TABLESPACE pg_default;
ALTER TABLE courthouse              ADD PRIMARY KEY USING INDEX courthouse_pk;

CREATE UNIQUE INDEX courthouse_region_ae_pk ON courthouse_region_ae(cth_id,reg_id) TABLESPACE pg_default;
ALTER TABLE courthouse_region_ae    ADD PRIMARY KEY USING INDEX courthouse_region_ae_pk;

CREATE UNIQUE INDEX courtroom_pk ON courtroom(ctr_id) TABLESPACE pg_default;
ALTER TABLE courtroom               ADD PRIMARY KEY USING INDEX courtroom_pk;

CREATE UNIQUE INDEX data_anonymisation_pk ON data_anonymisation(dan_id) TABLESPACE pg_default;
ALTER TABLE data_anonymisation              ADD PRIMARY KEY USING INDEX data_anonymisation_pk;

CREATE UNIQUE INDEX daily_list_pk ON daily_list(dal_id) TABLESPACE pg_default;
ALTER TABLE daily_list              ADD PRIMARY KEY USING INDEX daily_list_pk;

CREATE UNIQUE INDEX defence_pk    ON defence(dfc_id) TABLESPACE pg_default;
ALTER TABLE defence               ADD PRIMARY KEY USING INDEX defence_pk;

CREATE UNIQUE INDEX defendant_pk ON defendant(dfd_id) TABLESPACE pg_default;
ALTER TABLE defendant             ADD PRIMARY KEY USING INDEX defendant_pk;

CREATE UNIQUE INDEX event_pk ON event(eve_id) TABLESPACE pg_default;
ALTER TABLE event                   ADD PRIMARY KEY USING INDEX event_pk;

CREATE UNIQUE INDEX event_linked_case_pk ON event_linked_case(elc_id) TABLESPACE pg_default;
ALTER TABLE event_linked_case  ADD PRIMARY KEY USING INDEX event_linked_case_pk;

CREATE UNIQUE INDEX event_handler_pk ON event_handler(evh_id) TABLESPACE pg_default;
ALTER TABLE event_handler            ADD PRIMARY KEY USING INDEX event_handler_pk;

CREATE UNIQUE INDEX external_object_directory_pk ON external_object_directory(eod_id) TABLESPACE pg_default;
ALTER TABLE external_object_directory   ADD PRIMARY KEY USING INDEX external_object_directory_pk;

CREATE UNIQUE INDEX extobjdir_process_detail_pk ON extobjdir_process_detail(epd_id) TABLESPACE pg_default;
ALTER TABLE extobjdir_process_detail   ADD PRIMARY KEY USING INDEX extobjdir_process_detail_pk;

CREATE UNIQUE INDEX external_location_type_pk ON external_location_type(elt_id) TABLESPACE pg_default;
ALTER TABLE external_location_type   ADD PRIMARY KEY USING INDEX external_location_type_pk;

CREATE UNIQUE INDEX external_service_auth_token_pk ON external_service_auth_token(esa_id) TABLESPACE pg_default;
ALTER TABLE external_service_auth_token   ADD PRIMARY KEY USING INDEX external_service_auth_token_pk;

CREATE UNIQUE INDEX hearing_pk ON hearing(hea_id) TABLESPACE pg_default;
ALTER TABLE hearing                 ADD PRIMARY KEY USING INDEX hearing_pk;

CREATE UNIQUE INDEX hearing_annotation_ae_pk ON hearing_annotation_ae(hea_id,ann_id) TABLESPACE pg_default;
ALTER TABLE hearing_annotation_ae        ADD PRIMARY KEY USING INDEX hearing_annotation_ae_pk;

CREATE UNIQUE INDEX hearing_event_ae_pk ON hearing_event_ae(hea_id,eve_id) TABLESPACE pg_default;
ALTER TABLE hearing_event_ae        ADD PRIMARY KEY USING INDEX hearing_event_ae_pk;

CREATE UNIQUE INDEX hearing_judge_ae_pk ON hearing_judge_ae(hea_id,jud_id) TABLESPACE pg_default;
ALTER TABLE hearing_judge_ae        ADD PRIMARY KEY USING INDEX hearing_judge_ae_pk;

CREATE UNIQUE INDEX hearing_media_ae_pk ON hearing_media_ae(hea_id,med_id) TABLESPACE pg_default;
ALTER TABLE hearing_media_ae        ADD PRIMARY KEY USING INDEX hearing_media_ae_pk;

CREATE UNIQUE INDEX hearing_transcription_ae_pk ON hearing_transcription_ae(hea_id,tra_id) TABLESPACE pg_default;
ALTER TABLE hearing_transcription_ae        ADD PRIMARY KEY USING INDEX hearing_transcription_ae_pk;

CREATE UNIQUE INDEX judge_pk     ON judge(jud_id) TABLESPACE pg_default;
ALTER TABLE judge                ADD PRIMARY KEY USING INDEX judge_pk;

CREATE UNIQUE INDEX media_pk ON media(med_id) TABLESPACE pg_default;
ALTER TABLE media                   ADD PRIMARY KEY USING INDEX media_pk;

CREATE UNIQUE INDEX media_linked_case_pk ON media_linked_case(mlc_id) TABLESPACE pg_default;
ALTER TABLE media_linked_case  ADD PRIMARY KEY USING INDEX media_linked_case_pk;

CREATE UNIQUE INDEX media_request_pk ON media_request(mer_id) TABLESPACE pg_default;
ALTER TABLE media_request           ADD PRIMARY KEY USING INDEX media_request_pk;

CREATE UNIQUE INDEX node_register_pk ON node_register(node_id) TABLESPACE pg_default;
ALTER TABLE node_register         ADD PRIMARY KEY USING INDEX node_register_pk;

CREATE UNIQUE INDEX notification_pk ON notification(not_id) TABLESPACE pg_default;
ALTER TABLE notification            ADD PRIMARY KEY USING INDEX notification_pk;

CREATE UNIQUE INDEX object_admin_action_pk ON object_admin_action(oaa_id) TABLESPACE pg_default;
ALTER TABLE object_admin_action ADD PRIMARY KEY USING INDEX object_admin_action_pk;

CREATE UNIQUE INDEX object_hidden_reason_pk ON object_hidden_reason(ohr_id) TABLESPACE pg_default;
ALTER TABLE object_hidden_reason ADD PRIMARY KEY USING INDEX object_hidden_reason_pk;

CREATE UNIQUE INDEX object_record_status_pk ON object_record_status(ors_id) TABLESPACE pg_default;
ALTER TABLE object_record_status ADD PRIMARY KEY USING INDEX object_record_status_pk;

CREATE UNIQUE INDEX object_retrieval_queue_pk ON object_retrieval_queue(orq_id) TABLESPACE pg_default;
ALTER TABLE object_retrieval_queue ADD PRIMARY KEY USING INDEX object_retrieval_queue_pk;

CREATE UNIQUE INDEX prosecutor_pk ON prosecutor(prn_id) TABLESPACE pg_default;
ALTER TABLE prosecutor          ADD PRIMARY KEY USING INDEX prosecutor_pk;

CREATE UNIQUE INDEX region_pk ON region(reg_id) TABLESPACE pg_default;
ALTER TABLE region                  ADD PRIMARY KEY USING INDEX region_pk;

CREATE UNIQUE INDEX report_pk ON report(rep_id) TABLESPACE pg_default;
ALTER TABLE report                  ADD PRIMARY KEY USING INDEX report_pk;

CREATE UNIQUE INDEX transcription_pk ON transcription(tra_id) TABLESPACE pg_default;
ALTER TABLE transcription           ADD PRIMARY KEY USING INDEX transcription_pk;

CREATE UNIQUE INDEX transcription_comment_pk ON transcription_comment(trc_id) TABLESPACE pg_default;
ALTER TABLE transcription_comment   ADD PRIMARY KEY USING INDEX transcription_comment_pk;

CREATE UNIQUE INDEX transcription_document_pk ON transcription_document(trd_id) TABLESPACE pg_default;
ALTER TABLE transcription_document   ADD PRIMARY KEY USING INDEX transcription_document_pk;

CREATE UNIQUE INDEX transcription_linked_case_pk ON transcription_linked_case(tlc_id) TABLESPACE pg_default;
ALTER TABLE transcription_linked_case   ADD PRIMARY KEY USING INDEX transcription_linked_case_pk;

CREATE UNIQUE INDEX transcription_status_pk ON transcription_status(trs_id) TABLESPACE pg_default;
ALTER TABLE transcription_status      ADD PRIMARY KEY USING INDEX transcription_status_pk;

CREATE UNIQUE INDEX transcription_type_pk ON transcription_type(trt_id) TABLESPACE pg_default;
ALTER TABLE transcription_type      ADD PRIMARY KEY USING INDEX transcription_type_pk;

CREATE UNIQUE INDEX transcription_urgency_pk ON transcription_urgency(tru_id) TABLESPACE pg_default;
ALTER TABLE transcription_urgency                 ADD PRIMARY KEY USING INDEX transcription_urgency_pk;

CREATE UNIQUE INDEX transcription_workflow_pk ON transcription_workflow( trw_id) TABLESPACE pg_default;
ALTER TABLE transcription_workflow               ADD PRIMARY KEY USING INDEX transcription_workflow_pk;

CREATE UNIQUE INDEX transformed_media_pk ON transformed_media( trm_id) TABLESPACE pg_default;
ALTER TABLE transformed_media               ADD PRIMARY KEY USING INDEX transformed_media_pk;

CREATE UNIQUE INDEX transient_object_directory_pk ON transient_object_directory(tod_id) TABLESPACE pg_default;
ALTER TABLE transient_object_directory  ADD PRIMARY KEY USING INDEX transient_object_directory_pk;

CREATE UNIQUE INDEX user_account_pk ON user_account( usr_id) TABLESPACE pg_default;
ALTER TABLE user_account            ADD PRIMARY KEY USING INDEX user_account_pk;



-- defaults for postgres sequences, datatype->bigint, increment->1, nocycle is default, owned by none
CREATE SEQUENCE ann_seq CACHE 20;
CREATE SEQUENCE ado_seq CACHE 20;
CREATE SEQUENCE aat_seq CACHE 20;
CREATE SEQUENCE ard_seq CACHE 20;
CREATE SEQUENCE are_seq CACHE 20;
CREATE SEQUENCE aru_seq CACHE 20;
CREATE SEQUENCE aud_seq CACHE 20;
CREATE SEQUENCE aua_seq CACHE 20 RESTART WITH 8;
CREATE SEQUENCE aut_seq CACHE 20;
CREATE SEQUENCE cad_seq CACHE 20;
CREATE SEQUENCE cas_seq CACHE 20;
CREATE SEQUENCE cth_seq CACHE 1;
CREATE SEQUENCE ctr_seq CACHE 20;
CREATE SEQUENCE dal_seq CACHE 20;
CREATE SEQUENCE dan_seq CACHE 20;
CREATE SEQUENCE dfc_seq CACHE 20;
CREATE SEQUENCE dfd_seq CACHE 20;
CREATE SEQUENCE eve_seq CACHE 20;
CREATE SEQUENCE evh_seq CACHE 20;
CREATE SEQUENCE eod_seq CACHE 20;
CREATE SEQUENCE epd_seq CACHE 20;
CREATE SEQUENCE elt_seq CACHE 20;
CREATE SEQUENCE elc_seq CACHE 20;
CREATE SEQUENCE esa_seq CACHE 20;
CREATE SEQUENCE jud_seq CACHE 20;
CREATE SEQUENCE hea_seq CACHE 20;
CREATE SEQUENCE med_seq CACHE 20;
CREATE SEQUENCE mlc_seq CACHE 20;
CREATE SEQUENCE mer_seq CACHE 20;
CREATE SEQUENCE nod_seq CACHE 1 START WITH 50000;   -- sequence for node_register.node_id
CREATE SEQUENCE not_seq CACHE 20;
CREATE SEQUENCE oaa_seq CACHE 20;
CREATE SEQUENCE ohr_seq CACHE 20;
CREATE SEQUENCE ors_seq CACHE 1;
CREATE SEQUENCE orq_seq CACHE 20;
CREATE SEQUENCE prn_seq CACHE 20;
CREATE SEQUENCE reg_seq CACHE 20;
CREATE SEQUENCE rep_seq CACHE 20;
CREATE SEQUENCE tod_seq CACHE 20;
CREATE SEQUENCE tra_seq CACHE 20;
CREATE SEQUENCE trc_seq CACHE 20;
CREATE SEQUENCE trd_seq CACHE 20;
CREATE SEQUENCE tlc_seq CACHE 20;
CREATE SEQUENCE trw_seq CACHE 20;
CREATE SEQUENCE trm_seq CACHE 1;
CREATE SEQUENCE usr_seq CACHE 20;


-- foreign keys

ALTER TABLE annotation   
ADD CONSTRAINT annotation_current_owner_fk
FOREIGN KEY (current_owner) REFERENCES user_account(usr_id);

ALTER TABLE annotation   
ADD CONSTRAINT annotation_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation   
ADD CONSTRAINT annotation_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation   
ADD CONSTRAINT annotation_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation_document
ADD CONSTRAINT annotation_document_annotation_fk
FOREIGN KEY (ann_id) REFERENCES annotation(ann_id);

ALTER TABLE annotation_document 
ADD CONSTRAINT annotation_document_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation_document 
ADD CONSTRAINT annotation_document_uploaded_by_fk
FOREIGN KEY (uploaded_by) REFERENCES user_account(usr_id);

ALTER TABLE annotation_document 
ADD CONSTRAINT annotation_document_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE arm_automated_task 
ADD CONSTRAINT arm_automated_task_automated_task_fk
FOREIGN KEY (aut_id) REFERENCES automated_task(aut_id);

ALTER TABLE arm_rpo_execution_detail 
ADD CONSTRAINT arm_rpo_execution_detail_arm_state_fk
FOREIGN KEY (are_id) REFERENCES arm_rpo_state(are_id);

ALTER TABLE arm_rpo_execution_detail 
ADD CONSTRAINT arm_rpo_execution_detail_arm_status_fk
FOREIGN KEY (aru_id) REFERENCES arm_rpo_status(aru_id);

ALTER TABLE arm_rpo_execution_detail 
ADD CONSTRAINT arm_rpo_execution_detail_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE arm_rpo_execution_detail 
ADD CONSTRAINT arm_rpo_execution_detail_last_modified_by_fk
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

ALTER TABLE case_document            
ADD CONSTRAINT case_document_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_document            
ADD CONSTRAINT case_document_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE case_document            
ADD CONSTRAINT case_document_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_document            
ADD CONSTRAINT case_document_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE case_judge_ae            
ADD CONSTRAINT case_judge_ae_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_judge_ae            
ADD CONSTRAINT case_judge_ae_judge_fk
FOREIGN KEY (jud_id) REFERENCES judge(jud_id);

ALTER TABLE case_transcription_ae            
ADD CONSTRAINT case_transcription_ae_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_transcription_ae            
ADD CONSTRAINT case_transcription_ae_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

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

ALTER TABLE court_case  
ADD CONSTRAINT court_case_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE court_case  
ADD CONSTRAINT court_case_data_anonymised_by_fk
FOREIGN KEY (data_anonymised_by) REFERENCES user_account(usr_id);

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

ALTER TABLE data_anonymisation
ADD CONSTRAINT data_anonymisation_event_fk
FOREIGN KEY (eve_id) REFERENCES event(eve_id);

ALTER TABLE data_anonymisation
ADD CONSTRAINT data_anonymisation_transcription_comment_fk
FOREIGN KEY (trc_id) REFERENCES transcription_comment(trc_id);

ALTER TABLE data_anonymisation
ADD CONSTRAINT data_anonymisation_requested_by_fk
FOREIGN KEY (requested_by) REFERENCES user_account(usr_id);

ALTER TABLE data_anonymisation
ADD CONSTRAINT data_anonymisation_approved_by_fk
FOREIGN KEY (approved_by) REFERENCES user_account(usr_id);

ALTER TABLE daily_list
ADD CONSTRAINT daily_list_external_location_type_fk
FOREIGN KEY (elt_id) REFERENCES external_location_type(elt_id);

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

ALTER TABLE event_linked_case
ADD CONSTRAINT event_linked_case_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE event_linked_case
ADD CONSTRAINT event_linked_case_event_fk
FOREIGN KEY (eve_id) REFERENCES event(eve_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_transcription_document_fk
FOREIGN KEY (trd_id) REFERENCES transcription_document(trd_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_case_document_fk
FOREIGN KEY (cad_id) REFERENCES case_document(cad_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_annotation_document_fk
FOREIGN KEY (ado_id) REFERENCES annotation_document(ado_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT external_object_directory_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_object_record_status_fk
FOREIGN KEY (ors_id) REFERENCES object_record_status(ors_id);

ALTER TABLE external_object_directory   
ADD CONSTRAINT eod_external_location_type_fk
FOREIGN KEY (elt_id) REFERENCES external_location_type(elt_id);

ALTER TABLE extobjdir_process_detail  
ADD CONSTRAINT epd_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE extobjdir_process_detail  
ADD CONSTRAINT epd_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE extobjdir_process_detail  
ADD CONSTRAINT epd_external_object_directory_fk
FOREIGN KEY (eod_id) REFERENCES external_object_directory(eod_id);

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

ALTER TABLE hearing_annotation_ae            
ADD CONSTRAINT hearing_annotation_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_annotation_ae            
ADD CONSTRAINT hearing_annotation_ae_annotation_fk
FOREIGN KEY (ann_id) REFERENCES annotation(ann_id);

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

ALTER TABLE hearing_transcription_ae            
ADD CONSTRAINT hearing_transcription_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_transcription_ae            
ADD CONSTRAINT hearing_transcription_ae_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

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

ALTER TABLE media   
ADD CONSTRAINT media_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE media_linked_case   
ADD CONSTRAINT media_linked_case_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE media_linked_case   
ADD CONSTRAINT media_linked_case_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE media_linked_case   
ADD CONSTRAINT media_linked_case_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

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

ALTER TABLE node_register
ADD CONSTRAINT node_register_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE node_register
ADD CONSTRAINT node_register_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE notification                
ADD CONSTRAINT notification_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE notification
ADD CONSTRAINT notification_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE notification
ADD CONSTRAINT notification_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE object_admin_action   
ADD CONSTRAINT oaa_annotation_document_fk
FOREIGN KEY (ado_id) REFERENCES annotation_document(ado_id);

ALTER TABLE object_admin_action   
ADD CONSTRAINT oaa_case_document_fk
FOREIGN KEY (cad_id) REFERENCES case_document(cad_id);

ALTER TABLE object_admin_action   
ADD CONSTRAINT oaa_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE object_admin_action   
ADD CONSTRAINT oaa_transcription_document_fk
FOREIGN KEY (trd_id) REFERENCES transcription_document(trd_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT object_admin_action_ohr_id_fk
FOREIGN KEY (ohr_id) REFERENCES object_hidden_reason(ohr_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT object_admin_action_hidden_by_fk
FOREIGN KEY (hidden_by) REFERENCES user_account(usr_id);

ALTER TABLE object_admin_action
ADD CONSTRAINT object_admin_action_marked_for_manual_del_by_fk
FOREIGN KEY (marked_for_manual_del_by) REFERENCES user_account(usr_id);

ALTER TABLE object_retrieval_queue
ADD CONSTRAINT object_retrieval_queue_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE object_retrieval_queue
ADD CONSTRAINT object_retrieval_queue_transcription_document_fk
FOREIGN KEY (trd_id) REFERENCES transcription_document(trd_id);

ALTER TABLE object_retrieval_queue
ADD CONSTRAINT object_retrieval_queue_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE object_retrieval_queue
ADD CONSTRAINT object_retrieval_queue_last_modified_by_fk
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

ALTER TABLE report
ADD CONSTRAINT report_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE report
ADD CONSTRAINT report_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_transcription_status_fk
FOREIGN KEY (trs_id) REFERENCES transcription_status(trs_id);

ALTER TABLE transcription               
ADD CONSTRAINT transcription_urgency_fk
FOREIGN KEY (tru_id) REFERENCES transcription_urgency(tru_id);

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
ADD CONSTRAINT transcription_requested_by_fk
FOREIGN KEY (requested_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription   
ADD CONSTRAINT transcription_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_comment       
ADD CONSTRAINT transcription_comment_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_comment       
ADD CONSTRAINT transcription_comment_transcription_workflow_fk
FOREIGN KEY (trw_id) REFERENCES transcription_workflow(trw_id);

ALTER TABLE transcription_comment       
ADD CONSTRAINT transcription_comment_author_fk
FOREIGN KEY (author) REFERENCES user_account(usr_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_document
ADD CONSTRAINT transcription_document_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_document
ADD CONSTRAINT transcription_document_deleted_by_fk
FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_document
ADD CONSTRAINT transcription_document_uploaded_by_fk
FOREIGN KEY (uploaded_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_document
ADD CONSTRAINT transcription_document_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_linked_case
ADD CONSTRAINT transcription_linked_case_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE transcription_linked_case
ADD CONSTRAINT transcription_linked_case_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_workflow               
ADD CONSTRAINT transcription_workflow_transcription_status_fk
FOREIGN KEY (trs_id) REFERENCES transcription_status(trs_id);

ALTER TABLE transcription_workflow               
ADD CONSTRAINT transcription_workflow_workflow_actor_fk
FOREIGN KEY (workflow_actor) REFERENCES user_account(usr_id);

ALTER TABLE transformed_media  
ADD CONSTRAINT trm_media_request_fk
FOREIGN KEY (mer_id) REFERENCES media_request(mer_id);

ALTER TABLE transformed_media  
ADD CONSTRAINT transformed_media_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transformed_media  
ADD CONSTRAINT transformed_media_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory
ADD CONSTRAINT transient_object_directory_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory  
ADD CONSTRAINT tod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory  
ADD CONSTRAINT tod_transformed_media_fk
FOREIGN KEY (trm_id) REFERENCES transformed_media(trm_id);


ALTER TABLE transient_object_directory  
ADD CONSTRAINT tod_object_record_status_fk
FOREIGN KEY (ors_id) REFERENCES object_record_status(ors_id);



-- set defaults
ALTER TABLE node_register ALTER COLUMN node_type SET DEFAULT 'DAR';

-- additional check constraints

ALTER TABLE courthouse
ADD CONSTRAINT courthouse_name_ck CHECK (courthouse_name = UPPER(courthouse_name));

ALTER TABLE courtroom
ADD CONSTRAINT courtroom_name_ck CHECK (courtroom_name = UPPER(courtroom_name));

ALTER TABLE external_service_auth_token
ADD CONSTRAINT token_type_ck CHECK (token_type in (1,2));

-- additional multi-column not null constraints
ALTER TABLE data_anonymisation
ADD CONSTRAINT dan_one_of_eve_or_trc_nn
CHECK (eve_id is not null or trc_id is not null);

ALTER TABLE event_linked_case
ADD CONSTRAINT elc_modern_or_legacy_case_nn
CHECK ((cas_id is not null) or (courthouse_name is not null and case_number is not null));

ALTER TABLE external_object_directory
ADD CONSTRAINT eod_one_of_ado_cad_med_trd_nn
CHECK (ado_id is not null or cad_id is not null or med_id is not null or trd_id is not null);

ALTER TABLE media_linked_case
ADD CONSTRAINT mlc_modern_or_legacy_case_nn
CHECK ((cas_id is not null) or (courthouse_name is not null and case_number is not null));

ALTER TABLE object_admin_action
ADD CONSTRAINT oaa_one_of_ado_cad_med_trd_nn
CHECK (ado_id is not null or cad_id is not null or med_id is not null or trd_id is not null);

ALTER TABLE object_retrieval_queue
ADD CONSTRAINT orq_one_of_med_or_trd_nn
CHECK (med_id is not null or trd_id is not null);

ALTER TABLE transcription_linked_case
ADD CONSTRAINT tlc_modern_or_legacy_case_nn
CHECK ((cas_id is not null) or (courthouse_name is not null and case_number is not null));

-- additional unique multi-column indexes and constraints

--,UNIQUE (cth_id,courtroom_name)
CREATE UNIQUE INDEX ctr_chr_crn_unq ON courtroom( cth_id, courtroom_name) TABLESPACE pg_default;
ALTER TABLE courtroom ADD UNIQUE USING INDEX ctr_chr_crn_unq;

--,UNIQUE(cas_id,ctr_id,c_hearing_date)
CREATE UNIQUE INDEX hea_cas_ctr_hd_unq ON hearing( cas_id, ctr_id,hearing_date) TABLESPACE pg_default;
ALTER TABLE hearing ADD UNIQUE USING INDEX hea_cas_ctr_hd_unq;

--,UNIQUE(cth_id, case_number)
CREATE UNIQUE INDEX cas_case_number_cth_id_unq ON court_case(case_number,cth_id) TABLESPACE pg_default;
ALTER TABLE court_case ADD UNIQUE USING INDEX cas_case_number_cth_id_unq;

CREATE UNIQUE INDEX dfc_cas_id_defence_name_unq ON defence(cas_id,defence_name) TABLESPACE pg_default;
ALTER TABLE defence ADD UNIQUE USING INDEX dfc_cas_id_defence_name_unq;

CREATE UNIQUE INDEX dfd_cas_id_defendant_name_unq ON defendant(cas_id,defendant_name) TABLESPACE pg_default;
ALTER TABLE defendant ADD UNIQUE USING INDEX dfd_cas_id_defendant_name_unq;

CREATE UNIQUE INDEX prn_cas_id_prosecutor_name_unq ON prosecutor(cas_id,prosecutor_name) TABLESPACE pg_default;
ALTER TABLE prosecutor ADD UNIQUE USING INDEX prn_cas_id_prosecutor_name_unq;

CREATE UNIQUE INDEX user_account_user_email_address_unq
    ON user_account USING btree
    (upper(user_email_address) COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default
    WHERE is_active;

GRANT SELECT,INSERT,UPDATE,DELETE ON annotation TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON annotation_document TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON arm_automated_task TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON arm_rpo_execution_detail TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON arm_rpo_state TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON arm_rpo_status TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON audit TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON audit_activity TO darts_user;
GRANT SELECT                      ON audit_heritage TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON automated_task TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_document TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_judge_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_transcription_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON court_case TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON courthouse TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON courthouse_region_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON courtroom TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON data_anonymisation TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON daily_list TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON defence TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON defendant TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON event TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON event_handler TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON event_linked_case TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON external_location_type TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON external_object_directory TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON extobjdir_process_detail TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON external_service_auth_token TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing_annotation_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing_event_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing_judge_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing_media_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON hearing_transcription_ae TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON judge TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON media TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON media_linked_case TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON media_request TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON node_register TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON notification TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON object_admin_action TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON object_hidden_reason TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON object_record_status TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON object_retrieval_queue TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON prosecutor TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON region TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON report TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_comment TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_document TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_linked_case TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_status TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_type TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_urgency TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transcription_workflow TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transformed_media TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON transient_object_directory TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON user_account TO darts_user;

GRANT SELECT,UPDATE ON  ann_seq TO darts_user;
GRANT SELECT,UPDATE ON  aat_seq TO darts_user;
GRANT SELECT,UPDATE ON  ard_seq TO darts_user;
GRANT SELECT,UPDATE ON  are_seq TO darts_user;
GRANT SELECT,UPDATE ON  aru_seq TO darts_user;
GRANT SELECT,UPDATE ON  aua_seq TO darts_user;
GRANT SELECT,UPDATE ON  aud_seq TO darts_user;
GRANT SELECT,UPDATE ON  aut_seq TO darts_user;
GRANT SELECT,UPDATE ON  cas_seq TO darts_user;
GRANT SELECT,UPDATE ON  cth_seq TO darts_user;
GRANT SELECT,UPDATE ON  ctr_seq TO darts_user;
GRANT SELECT,UPDATE ON  dal_seq TO darts_user;
GRANT SELECT,UPDATE ON  dan_seq TO darts_user;
GRANT SELECT,UPDATE ON  dfc_seq TO darts_user;
GRANT SELECT,UPDATE ON  dfd_seq TO darts_user;
GRANT SELECT,UPDATE ON  elt_seq TO darts_user;
GRANT SELECT,UPDATE ON  elc_seq TO darts_user;
GRANT SELECT,UPDATE ON  eod_seq TO darts_user;
GRANT SELECT,UPDATE ON  epd_seq TO darts_user;
GRANT SELECT,UPDATE ON  esa_seq TO darts_user;
GRANT SELECT,UPDATE ON  eve_seq TO darts_user;
GRANT SELECT,UPDATE ON  evh_seq TO darts_user;
GRANT SELECT,UPDATE ON  hea_seq TO darts_user;
GRANT SELECT,UPDATE ON  jud_seq TO darts_user;
GRANT SELECT,UPDATE ON  med_seq TO darts_user;
GRANT SELECT,UPDATE ON  mer_seq TO darts_user;
GRANT SELECT,UPDATE ON  mlc_seq TO darts_user;
GRANT SELECT,UPDATE ON  nod_seq TO darts_user;
GRANT SELECT,UPDATE ON  not_seq TO darts_user;
GRANT SELECT,UPDATE ON  ors_seq TO darts_user;
GRANT SELECT,UPDATE ON  prn_seq TO darts_user;
GRANT SELECT,UPDATE ON  reg_seq TO darts_user;
GRANT SELECT,UPDATE ON  rep_seq TO darts_user;
GRANT SELECT,UPDATE ON  tod_seq TO darts_user;
GRANT SELECT,UPDATE ON  tlc_seq TO darts_user;
GRANT SELECT,UPDATE ON  tra_seq TO darts_user;
GRANT SELECT,UPDATE ON  trc_seq TO darts_user;
GRANT SELECT,UPDATE ON  trw_seq TO darts_user;
GRANT SELECT,UPDATE ON  trm_seq TO darts_user;
GRANT SELECT,UPDATE ON  usr_seq TO darts_user;

GRANT USAGE ON SCHEMA DARTS TO darts_user;

CREATE OR REPLACE FUNCTION tra_trw_sync_fnc()
RETURNS trigger AS
$$
BEGIN
UPDATE transcription SET trs_id = NEW.trs_id WHERE tra_id = NEW.tra_id;
RETURN NEW;
END;
$$
LANGUAGE 'plpgsql';

CREATE OR REPLACE TRIGGER trw_ar_trg
AFTER INSERT ON transcription_workflow
FOR EACH ROW
EXECUTE PROCEDURE tra_trw_sync_fnc();

SET ROLE DARTS_USER;
SET SEARCH_PATH TO darts;








