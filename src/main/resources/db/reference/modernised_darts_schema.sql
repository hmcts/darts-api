--v6 add sequences,remove character/numeric size limits, change DATE to TIMESTAMP
--v7 consistently add the legacy primary and foreign keys
--v8 3NF courthouses
--v9 remove various legacy columns
--v10 change some numeric columns to boolean, remove unused legacy column c_upload_priority
--v11 introduce many:many case:hearing, removed version label & superceded from moj_hearing, as no source for migration, and assume unneeded by modernised
--v12 remove reporting_restrictions from annotation,cached_media,event,media,transcription,transformation_request
--    add message_id, event_type_id to moj_event
--    add moj_event_type table and links to moj_event by FK
--v13 adding Not null to moj_transcription FK moj_cas_id & moj_crt_id,
--    adding moj_transcription_type table to replace c_type. Remove fields c_notification_type, c_urgent from transcription
--    add event_name to moj_event
--    add moj_urgency table and fk to moj_transcription
--    added comment_ts and author to moj_transcription_comment
--v14 removing unneeded columns from moj_courthouse, normalising crown court code from daily list
--    amending judge,defendant,defence, prosecutor on hearing to be 1-d array instead of scalar
--    rename i_version_label to i_version
--v15 remove moj_crt_id from case and corresponding FK
--v16 add moj_hea_id to transcription and corresponding FK
--    add moj_user to this script
--v17 further comments reagrding properties of live data
--v18 moving atributes from moj_hearing to moj_case, changing timestamps to ts with tz
--v19 amended courthouse_name to be unique, amended courthouse_code to be integer
--    removing c_scheduled_start from moj_case, to be replaced by 2 columns on moj_hearing, scheduled_start_time and hearing_is_actual flag
--v20 moving moj_event and moj_media to link to moj_hearing rather than moj_case, resulting in moj_case_event_ae and
--    moj_case_media_ae changing name
--v21 normalising c_reporting_restrictions into moj_reporting_restriction table
--    change alias for courthouse from CRT to CTH, accommodate new COURTROOM table aliased to CTR
--    add COURTROOM table, replace existing FKs to COURTHOUSE with ones to COURTROOM for event, media
--    rename moj_event_type.type to .evt_type
--    remove c_courtroom from moj_annotation,,moj_cached_media, moj_event, moj_hearing, moj_media,
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
--    added device_register table ( equivalent to legacy tbl_moj_node)
--    added unique constraint on court_case(cth_id, case_number
--    standardised the use of "last_modified_ts" , where previously using "modified_ts" or "last_updated_ts"
--    standardised the use of "last_modified_by" , where previously using "modified_by"
--    reduced number of Documentum columns on user_account table, while adding a few others




-- List of Table Aliases
-- annotation                 ANN
-- court_case                 CAS
-- courthouse                 CTH
-- courthouse_region_ae       CRA
-- courtroom                  CTR
-- daily_list                 DAL
-- device_register            DER
-- event                      EVE
-- event_handler              EVH
-- external_object_directory  EOD
-- hearing                    HEA
-- hearing_event_ae           HEV
-- hearing_media_ae           HMA
-- media                      MED
-- media_request              MER
-- notification               NOT
-- object_directory_status    ODS
-- region                     REG
-- report                     REP
-- reporting_restrictions     RER
-- transcription              TRA
-- transcription_type         TRT
-- transient_object_directory TOD
-- urgency                    URG
-- user_account               USR

CREATE SCHEMA DARTS;
CREATE TABLESPACE darts_tables  location 'E:/PostgreSQL/Tables';
CREATE TABLESPACE darts_indexes location 'E:/PostgreSQL/Indexes';


CREATE TABLE darts.annotation
(ann_id                   INTEGER					 NOT NULL
,cas_id                   INTEGER					 NOT NULL
,ctr_id                   INTEGER
,annotation_text          CHARACTER VARYING
,annotation_ts            TIMESTAMP WITH TIME ZONE
,annotation_object_id     CHARACTER VARYING(16)
,version_label            CHARACTER VARYING(32)
,superseded               BOOLEAN
,version                  INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.annotation.ann_id
IS 'primary key of annotation';

COMMENT ON COLUMN darts.annotation.cas_id
IS 'foreign key from court_case';

COMMENT ON COLUMN darts.annotation.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.annotation.annotation_object_id
IS 'internal Documentum primary key from moj_annotation_s';

COMMENT ON COLUMN darts.annotation.annotation_text
IS 'directly sourced from moj_annotation_s.c_text';

COMMENT ON COLUMN darts.annotation.annotation_ts
IS 'directly sourced from moj_annotation_s';

COMMENT ON COLUMN darts.annotation.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_annotation';

CREATE TABLE darts.court_case
(cas_id                    INTEGER					 NOT NULL
,cth_id                    INTEGER                   NOT NULL
,rer_id                    INTEGER
,case_object_id            CHARACTER VARYING(16)
,case_number               CHARACTER VARYING     -- maps to c_case_id in legacy
,case_closed               BOOLEAN
,interpreter_used          BOOLEAN
,case_closed_ts            TIMESTAMP WITH TIME ZONE
,defendant_list            CHARACTER VARYING[]
,prosecutor_list           CHARACTER VARYING[]
,defence_list              CHARACTER VARYING[]
,retain_until_ts           TIMESTAMP WITH TIME ZONE
,version_label             CHARACTER VARYING(32)
,superseded                BOOLEAN
,version                   INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.court_case.cas_id
IS 'primary key of court_case';

COMMENT ON COLUMN darts.court_case.cth_id
IS 'foreign key to courthouse';

COMMENT ON COLUMN darts.court_case.rer_id
IS 'foreign key to reporting_restrictions';

COMMENT ON COLUMN darts.court_case.case_object_id
IS 'internal Documentum primary key from moj_case_s';

COMMENT ON COLUMN darts.court_case.case_number
IS 'directly sourced from moj_case_s.c_case_id';

COMMENT ON COLUMN darts.court_case.case_closed
IS 'migrated from moj_case_s, converted from numeric to boolean';

COMMENT ON COLUMN darts.court_case.interpreter_used
IS 'migrated from moj_case_s, converted from numeric to boolean';

COMMENT ON COLUMN darts.court_case.case_closed_ts
IS 'directly sourced from moj_case_s.c_case_closed_date';

COMMENT ON COLUMN darts.court_case.defendant_list
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN darts.court_case.prosecutor_list
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN darts.court_case.defence_list
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN darts.court_case.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_case, containing the version record';

COMMENT ON COLUMN darts.court_case.version
IS 'inherited from dm_sysobject_r, for r_object_type of moj_case, set according to the current flag';

CREATE TABLE darts.courthouse
(cth_id                    INTEGER					 NOT NULL
,courthouse_code           INTEGER                                     UNIQUE
,courthouse_name           CHARACTER VARYING         NOT NULL          UNIQUE
,created_ts                TIMESTAMP WITH TIME ZONE  NOT NULL
,last_modified_ts          TIMESTAMP WITH TIME ZONE  NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.courthouse.cth_id
IS 'primary key of courthouse';

COMMENT ON COLUMN darts.courthouse.courthouse_code
IS 'corresponds to the c_crown_court_code found in daily lists';

COMMENT ON COLUMN darts.courthouse.courthouse_name
IS 'directly sourced from moj_courthouse_s.c_id';

CREATE TABLE darts.courthouse_region_ae
(cra_id                     INTEGER                  NOT NULL
,cth_id                     INTEGER                  NOT NULL
,reg_id                     INTEGER                  NOT NULL
) TABLESPACE darts_tables;

CREATE TABLE darts.courtroom
(ctr_id                     INTEGER                  NOT NULL
,cth_id                     INTEGER                  NOT NULL
,courtroom_name             CHARACTER VARYING        NOT NULL
--,UNIQUE(moj_cth_id,courtroom_name)
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.courtroom.ctr_id
IS 'primary key of courtroom';

COMMENT ON COLUMN darts.courtroom.cth_id
IS 'foreign key to courthouse';

CREATE TABLE darts.daily_list
(dal_id                     INTEGER					 NOT NULL
,cth_id                     INTEGER					 NOT NULL
,daily_list_object_id       CHARACTER VARYING(16)
,unique_id                  CHARACTER VARYING
--,c_crown_court_name       CHARACTER VARYING        -- removed, normalised to courthouses, but note that in legacy there is mismatch between moj_courthouse_s.c_id and moj_daily_list_s.c_crown_court_name to be resolved
,job_status                 CHARACTER VARYING        -- one of "New","Partially Processed","Processed","Ignored","Invalid"
,published_ts               TIMESTAMP WITH TIME ZONE
,start_dt                   DATE
,end_dt                     DATE -- all values match c_start_date
,daily_list_id_s            CHARACTER VARYING        -- non unique integer in legacy
,daily_list_source          CHARACTER VARYING        -- one of CPP,XHB ( live also sees nulls and spaces)
,daily_list_content         CHARACTER VARYING
,created_ts                 TIMESTAMP WITH TIME ZONE
,last_modified_ts           TIMESTAMP WITH TIME ZONE
,version_label              CHARACTER VARYING(32)
,superseded                 BOOLEAN
,version                    INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.daily_list.dal_id
IS 'primary key of daily_list';

COMMENT ON COLUMN darts.daily_list.cth_id
IS 'foreign key from courthouse';

COMMENT ON COLUMN darts.daily_list.daily_list_object_id
IS 'internal Documentum primary key from moj_daily_list_s';

COMMENT ON COLUMN darts.daily_list.unique_id
IS 'directly sourced from moj_daily_list_s, received as part of the XML, used to find duplicate daily lists';

COMMENT ON COLUMN darts.daily_list.job_status
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN darts.daily_list.published_ts
IS 'directly sourced from moj_daily_list_s.c_timestamp';

COMMENT ON COLUMN darts.daily_list.start_dt
IS 'directly sourced from moj_daily_list_s.c_start_date';

COMMENT ON COLUMN darts.daily_list.end_dt
IS 'directly sourced from moj_daily_list_s.c_end_date';

COMMENT ON COLUMN darts.daily_list.daily_list_id_s
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN darts.daily_list.daily_list_source
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN darts.daily_list.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_daily_list';

CREATE TABLE darts.device_register
(der_id                     INTEGER                  NOT NULL
,ctr_id                     INTEGER                  NOT NULL
,node_id                    INTEGER
,hostname                   CHARACTER VARYING
,ip_address                 CHARACTER VARYING
,mac_address                CHARACTER VARYING
) TABLESPACE darts_tables;

COMMENT ON TABLE darts.device_register
IS 'corresponds to tbl_moj_node from legacy';

COMMENT ON COLUMN darts.device_register.der_id
IS 'primary key of device_register';

COMMENT ON COLUMN darts.device_register.ctr_id
IS 'foreign key from moj_courtroom, legacy stored courthouse and courtroon un-normalised';




CREATE TABLE darts.event
(eve_id                     INTEGER					 NOT NULL
,ctr_id                     INTEGER
,evh_id                     INTEGER
,event_object_id            CHARACTER VARYING(16)
,event_id                   NUMERIC
,event_name                 CHARACTER VARYING -- details of the handler, at point in time the event arose
,event_text                 CHARACTER VARYING
,event_ts                   TIMESTAMP WITH TIME ZONE
,case_number                CHARACTER VARYING(32)[]
,version_label              CHARACTER VARYING(32)
,message_id                 CHARACTER VARYING
,superseded                 BOOLEAN
,version                    INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.event.eve_id
IS 'primary key of moj_event';

COMMENT ON COLUMN darts.event.ctr_id
IS 'foreign key from moj_courtroom';

COMMENT ON COLUMN darts.event.evh_id
IS 'foreign key for the moj_event_handler table';

COMMENT ON COLUMN darts.event.event_object_id
IS 'internal Documentum primary key from moj_event_s';

COMMENT ON COLUMN darts.event.event_id
IS 'directly sourced from moj_event_s';

COMMENT ON COLUMN darts.event.event_name
IS 'inherited from dm_sysobect_s.object_name';

COMMENT ON COLUMN darts.event.event_text
IS 'inherited from moj_annotation_s.c_text';

COMMENT ON COLUMN darts.event.event_ts
IS 'inherited from moj_annotation_s';

COMMENT ON COLUMN darts.event.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_event';

COMMENT ON COLUMN darts.event.message_id
IS 'no migration element, records the id of the message that gave rise to this event';


CREATE TABLE darts.event_handler
(evh_id                      INTEGER					 NOT NULL
,event_type                  CHARACTER VARYING           NOT NULL
,event_sub_type              CHARACTER VARYING
,event_name                  CHARACTER VARYING           NOT NULL
,handler                     CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE    NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE    NOT NULL
,last_modified_by            INTEGER                     NOT NULL
) TABLESPACE darts_tables;

COMMENT ON TABLE darts.event_handler
IS 'content will be derived from TBL_MOJ_DOC_HANDLER in the legacy database, but currently has no primary key and 6 fully duplicated rows';

COMMENT ON COLUMN darts.event_handler.evh_id
IS 'primary key of moj_event_type';

COMMENT ON COLUMN darts.event_handler.event_type
IS 'directly sourced from doc_type';

COMMENT ON COLUMN darts.event_handler.event_sub_type
IS 'directly sourced from doc_sub_type';

COMMENT ON COLUMN darts.event_handler.event_name
IS 'directly sourced from event_name';

COMMENT ON COLUMN darts.event_handler.handler
IS 'directly sourced from doc_handler';


CREATE TABLE darts.external_object_directory
(eod_id                      INTEGER			 		 NOT NULL
,med_id                      INTEGER
,tra_id                      INTEGER
,ann_id                      INTEGER
,ods_id                      INTEGER                     NOT NULL  -- FK to moj_object_directory_status
,elt_id                      INTEGER                     NOT NULL  -- one of inbound,unstructured,arm,tempstore,vodafone
-- additional optional FKs to other relevant internal objects would require columns here
,external_location           UUID                        NOT NULL
,checksum	                 CHARACTER VARYING
,transfer_attempts           INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE    NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE    NOT NULL
,last_modified_by            INTEGER                     NOT NULL  -- FK to moj_user.moj_usr_id
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.external_object_directory.eod_id
IS 'primary key of external_object_directory';

COMMENT ON COLUMN darts.external_object_directory.elt_id
IS 'foreign key from external_location_type';

-- added two foreign key columns, but there will be as many FKs as there are distinct objects with externally stored components

COMMENT ON COLUMN darts.external_object_directory.med_id
IS 'foreign key from media';

COMMENT ON COLUMN darts.external_object_directory.tra_id
IS 'foreign key from transcription';

COMMENT ON COLUMN darts.external_object_directory.ann_id
IS 'foreign key from annotation';

CREATE TABLE darts.external_location_type
(elt_id                     INTEGER                  NOT NULL
,elt_description            CHARACTER VARYING
) TABLESPACE darts_tables;

COMMENT ON TABLE darts.external_location_type
IS 'used to record acceptable external locations, found in external_object_directory';

CREATE TABLE darts.hearing
(hea_id                     INTEGER					   NOT NULL
,cas_id                     INTEGER                    NOT NULL
,ctr_id                     INTEGER                    NOT NULL
,judge_list                 CHARACTER VARYING[]
,hearing_date               DATE     -- to record only DATE component of hearings, both scheduled and actual
,scheduled_start_time       TIME     -- to record only TIME component of hearings, while they are scheduled only
,hearing_is_actual          BOOLEAN  -- TRUE for actual hearings, FALSE for scheduled hearings
,judge_hearing_date         CHARACTER VARYING
--,UNIQUE(moj_cas_id,moj_ctr,c_hearing_date)
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.hearing.hea_id
IS 'primary key of hearing';

COMMENT ON COLUMN darts.hearing.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN darts.hearing.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.hearing.judge_list
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN darts.hearing.hearing_date
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN darts.hearing.judge_hearing_date
IS 'directly sourced from moj_case_r';

CREATE TABLE darts.hearing_event_ae
(hev_id                     INTEGER							 NOT NULL
,hea_id                     INTEGER							 NOT NULL
,eve_id                     INTEGER							 NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.hearing_event_ae.hev_id
IS 'primary key of hearing_event_ae';

COMMENT ON COLUMN darts.hearing_event_ae.hea_id
IS 'foreign key from hearing, part of composite natural key';

COMMENT ON COLUMN darts.hearing_event_ae.eve_id
IS 'foreign key from event, part of composite natural key';

CREATE TABLE darts.hearing_media_ae
(hma_id                     INTEGER 						 NOT NULL
,hea_id                     INTEGER							 NOT NULL
,med_id                     INTEGER							 NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.hearing_media_ae.hma_id
IS 'primary key of hearing_media_ae';

COMMENT ON COLUMN darts.hearing_media_ae.hea_id
IS 'foreign key from case, part of composite natural key';

COMMENT ON COLUMN darts.hearing_media_ae.med_id
IS 'foreign key from media, part of composite natural key';

CREATE TABLE darts.media
(med_id                     INTEGER					 NOT NULL
,ctr_id                     INTEGER
,media_object_id            CHARACTER VARYING(16)
,channel                    INTEGER
,total_channels             INTEGER                  --99.9% are "4" in legacy
,reference_id               CHARACTER VARYING        --all nulls in legacy
,start_ts                   TIMESTAMP WITH TIME ZONE
,end_ts                     TIMESTAMP WITH TIME ZONE
,case_number                CHARACTER VARYING(32)[]  --this is a placeholder for moj_case_document_r.c_case_id, known to be repeated for moj_media object types
,version_label              CHARACTER VARYING(32)
,superseded                 BOOLEAN
,version                    INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.media.med_id
IS 'primary key of media';

COMMENT ON COLUMN darts.media.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.media.media_object_id
IS 'internal Documentum primary key from moj_media_s';

COMMENT ON COLUMN darts.media.channel
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN darts.media.total_channels
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN darts.media.reference_id
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN darts.media.start_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN darts.media.end_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN darts.media.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_media';

CREATE TABLE darts.media_request
(mer_id                     INTEGER                    NOT NULL
,hea_id                     INTEGER                    NOT NULL
,requestor                  INTEGER                    NOT NULL  -- FK to moj_user.moj_usr_id
,request_status             CHARACTER VARYING
,request_type               CHARACTER VARYING
,req_proc_attempts          INTEGER
,start_ts                   TIMESTAMP WITH TIME ZONE
,end_ts                     TIMESTAMP WITH TIME ZONE
,created_ts                 TIMESTAMP WITH TIME ZONE
,last_modified_ts            TIMESTAMP WITH TIME ZONE
,last_accessed_ts           TIMESTAMP WITH TIME ZONE
,output_filename            CHARACTER VARYING
,output_format              CHARACTER VARYING
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.media_request.mer_id
IS 'primary key of media_request';

COMMENT ON COLUMN darts.media_request.hea_id
IS 'foreign key of hearing';

COMMENT ON COLUMN darts.media_request.requestor
IS 'requestor of the media request, possibly migrated from moj_transformation_request_s';

COMMENT ON COLUMN darts.media_request.request_status
IS 'status of the migration request';

COMMENT ON COLUMN darts.media_request.req_proc_attempts
IS 'number of attempts by ATS to process the request';

COMMENT ON COLUMN darts.media_request.start_ts
IS 'start time in the search criteria for request, possibly migrated from moj_cached_media_s or moj_transformation_request_s';

COMMENT ON COLUMN darts.media_request.end_ts
IS 'end time in the search criteria for request, possibly migrated from moj_cached_media_s or moj_transformation_request_s';

COMMENT ON COLUMN darts.media_request.output_filename
IS 'filename of the requested media object, possibly migrated from moj_transformation_request_s';

COMMENT ON COLUMN darts.media_request.output_format
IS 'format of the requested media object, possibly migrated from moj_transformation_s';



CREATE TABLE darts.notification
(not_id                     INTEGER                    NOT NULL
,cas_id                     INTEGER                    NOT NULL
,notification_event   		CHARACTER VARYING          NOT NULL
,notification_status        CHARACTER VARYING          NOT NULL
,email_address              CHARACTER VARYING          NOT NULL
,send_attempts              INTEGER
,template_values            CHARACTER VARYING
,created_ts                 TIMESTAMP WITH TIME ZONE   NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE   NOT NULL
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.notification.not_id
IS 'primary key of notification';

COMMENT ON COLUMN darts.notification.cas_id
IS 'foreign key to case';

COMMENT ON COLUMN darts.notification.notification_event
IS 'event giving rise to the need for outgoing notification';

COMMENT ON COLUMN darts.notification.notification_status
IS 'status of the notification, expected to be one of [O]pen, [P]rocessing, [S]end, [F]ailed';

COMMENT ON COLUMN darts.notification.email_address
IS 'recipient of the notification';

COMMENT ON COLUMN darts.notification.send_attempts
IS 'number of outgoing requests to gov.uk';

COMMENT ON COLUMN darts.notification.template_values
IS 'any extra fields not already covered or inferred from the case, in JSON format';


CREATE TABLE darts.object_directory_status
(ods_id                     INTEGER                  NOT NULL
,ods_description            CHARACTER VARYING
) TABLESPACE darts_tables;

COMMENT ON TABLE darts.object_directory_status
IS 'used to record acceptable statuses found in [external/transient]_object_directory';

CREATE TABLE darts.region
(reg_id                     INTEGER
,region_name                CHARACTER VARYING
) TABLESPACE darts_tables;


CREATE TABLE darts.report
(rep_id                     INTEGER					 NOT NULL
,report_object_id           CHARACTER VARYING(16)
,name                       CHARACTER VARYING
,subject                    CHARACTER VARYING
,report_text                CHARACTER VARYING
,query                      CHARACTER VARYING
,recipients                 CHARACTER VARYING
,version_label              CHARACTER VARYING(32)
,superseded                 BOOLEAN
,version                    INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.report.rep_id
IS 'primary key of report';

COMMENT ON COLUMN darts.report.report_object_id
IS 'internal Documentum primary key from moj_report_s';

COMMENT ON COLUMN darts.report.name
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN darts.report.subject
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN darts.report.report_text
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN darts.report.query
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN darts.report.recipients
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN darts.report.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_report';

CREATE TABLE darts.reporting_restrictions
(rer_id                     INTEGER                  NOT NULL
,rer_description            CHARACTER VARYING
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.reporting_restrictions.rer_id
IS 'primary key of reporting_restrictions';

COMMENT ON COLUMN darts.reporting_restrictions.rer_description
IS 'text of the relevant legislation, to be populated from moj_case_s.c_reporting_restrictions';

CREATE TABLE darts.transcription
(tra_id                   INTEGER				   NOT NULL
,cas_id                   INTEGER                  NOT NULL
,ctr_id                   INTEGER                  NOT NULL
,trt_id                   INTEGER                  NOT NULL
,urg_id                   INTEGER                  -- remains nullable, as nulls present in source data ( c_urgency)
,hea_id                   INTEGER                  -- remains nullable, until migration is complete
,transcription_object_id  CHARACTER VARYING(16)    -- legacy pk from moj_transcription_s.r_object_id
,company                  CHARACTER VARYING        -- effectively unused in legacy, either null or "<this field will be completed by the system>"
,requestor                CHARACTER VARYING        -- 1055 distinct, from <forname><surname> to <AAANNA>
,current_state            CHARACTER VARYING        -- 23 distinct, far more than 5 expected (requested,awaiting authorisation,with transcribed, complete, rejected)
,current_state_ts         TIMESTAMP WITH TIME ZONE -- date & time record entered the current c_current_state
,hearing_date             TIMESTAMP WITH TIME ZONE -- 3k records have time component, but all times are 23:00,so effectively DATE only, will be absolete once moj_hea_id populated
,start_ts                 TIMESTAMP WITH TIME ZONE -- both c_start and c_end have time components
,end_ts                   TIMESTAMP WITH TIME ZONE -- we have 49k rows in legacy moj_transcription_s, 7k have c_end != c_start
,created_ts               TIMESTAMP WITH TIME ZONE
,last_modified_ts         TIMESTAMP WITH TIME ZONE
,last_modified_by         INTEGER                  -- will need to be FK to users table
,requested_by             INTEGER                  -- will need to be FK to users table
,approved_by              INTEGER                  -- will need to be FK to users table
,approved_on_ts           TIMESTAMP WITH TIME ZONE
,transcribed_by           INTEGER                  -- will need to be FK to users table
,version_label            CHARACTER VARYING(32)
,superseded               BOOLEAN
,version                  INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.transcription.tra_id
IS 'primary key of transcription';

COMMENT ON COLUMN darts.transcription.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN darts.transcription.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.transcription.urg_id
IS 'foreign key from urgency';

COMMENT ON COLUMN darts.transcription.trt_id
IS 'foreign key to transcription_type, sourced from moj_transcription_s.c_type';

COMMENT ON COLUMN darts.transcription.transcription_object_id
IS 'internal Documentum primary key from moj_transcription_s';

COMMENT ON COLUMN darts.transcription.company
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN darts.transcription.requestor
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN darts.transcription.current_state
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN darts.transcription.hearing_date
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN darts.transcription.start_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN darts.transcription.end_ts
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN darts.transcription.version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_transcription';

CREATE TABLE darts.transcription_comment
(trc_id                            INTEGER					 NOT NULL
,tra_id                            INTEGER
,transcription_object_id           CHARACTER VARYING(16)     -- this is a placeholder for moj_transcription_s.r_object_id
,transcription_comment             CHARACTER VARYING
,comment_ts                        TIMESTAMP WITH TIME ZONE
,author                            INTEGER                   -- will need to be FK to user table
,created_ts                        TIMESTAMP WITH TIME ZONE
,last_modified_ts                  TIMESTAMP WITH TIME ZONE
,last_modified_by                  INTEGER                   -- will need to be FK to users table
,superseded                        BOOLEAN
,version                           INTEGER
) TABLESPACE darts_tables;

COMMENT ON COLUMN darts.transcription_comment.trc_id
IS 'primary key of transcription_comment';

COMMENT ON COLUMN darts.transcription_comment.tra_id
IS 'foreign key from transcription';

COMMENT ON COLUMN darts.transcription_comment.transcription_object_id
IS 'internal Documentum primary key from moj_transcription_s';

COMMENT ON COLUMN darts.transcription_comment.transcription_comment
IS 'directly sourced from moj_transcription_r';

COMMENT ON COLUMN darts.transcription_comment.transcription_object_id
IS 'internal Documentum id from moj_transcription_s acting as foreign key';

CREATE TABLE darts.transcription_type
(trt_id                            INTEGER                   NOT NULL
,description                       CHARACTER VARYING
);

COMMENT ON TABLE darts.transcription_type
IS 'standing data table, migrated from tbl_moj_transcription_type';

COMMENT ON COLUMN darts.transcription_type.trt_id
IS 'primary key, but not sequence generated';

CREATE TABLE darts.transient_object_directory
(tod_id                      INTEGER			 		 NOT NULL
,mer_id                      INTEGER                     NOT NULL
,ods_id                      INTEGER                     NOT NULL  -- FK to moj_object_directory_status.moj_ods_id
,external_location           UUID                        NOT NULL
,checksum	                 CHARACTER VARYING
,transfer_attempts           INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE    NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE    NOT NULL
,last_modified_by            INTEGER                     NOT NULL  -- FK to moj_user.moj_usr_id
) TABLESPACE darts_tables;

CREATE TABLE darts.urgency
(urg_id                            INTEGER                 NOT NULL
,description                       CHARACTER VARYING
) TABLESPACE darts_tables;

COMMENT ON TABLE darts.urgency
IS 'will be migrated from tbl_moj_urgency';

COMMENT ON COLUMN darts.urgency.urg_id
IS 'inherited from tbl_moj_urgency.urgency_id';

COMMENT ON COLUMN darts.urgency.description
IS 'inherited from tbl_moj_urgency.description';

CREATE TABLE darts.user_account
(usr_id                  INTEGER
,dm_user_s_object_id     CHARACTER VARYING(16)
,user_name               CHARACTER VARYING
,user_email_address      CHARACTER VARYING
,description             CHARACTER VARYING
,user_state              NUMERIC
,created_ts              TIMESTAMP WITH TIME ZONE
,last_modified_ts        TIMESTAMP WITH TIME ZONE
,last_login_ts           TIMESTAMP WITH TIME ZONE
,last_modified_by        INTEGER
) TABLESPACE darts_tables;

COMMENT ON TABLE darts.user_account
IS 'migration columns all sourced directly from dm_user_s, but only for rows where r_is_group = 0';
COMMENT ON COLUMN darts.user_account.usr_id
IS 'primary key of user_account';
COMMENT ON COLUMN darts.user_account.dm_user_s_object_id
IS 'internal Documentum primary key from dm_user_s';

-- primary keys

CREATE UNIQUE INDEX annotation_pk ON darts.annotation(ann_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.annotation              ADD PRIMARY KEY USING INDEX annotation_pk;

CREATE UNIQUE INDEX court_case_pk ON darts.court_case(cas_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.court_case              ADD PRIMARY KEY USING INDEX court_case_pk;

CREATE UNIQUE INDEX courthouse_pk ON darts.courthouse(cth_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.courthouse              ADD PRIMARY KEY USING INDEX courthouse_pk;

CREATE UNIQUE INDEX courthouse_region_ae_pk ON darts.courthouse_region_ae(cra_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.courthouse_region_ae    ADD PRIMARY KEY USING INDEX courthouse_region_ae_pk;

CREATE UNIQUE INDEX courtroom_pk ON darts.courtroom(ctr_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.courtroom               ADD PRIMARY KEY USING INDEX courtroom_pk;

CREATE UNIQUE INDEX daily_list_pk ON darts.daily_list(dal_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.daily_list              ADD PRIMARY KEY USING INDEX daily_list_pk;

CREATE UNIQUE INDEX device_register_pk ON darts.device_register(der_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.device_register              ADD PRIMARY KEY USING INDEX device_register_pk;

CREATE UNIQUE INDEX event_pk ON darts.event(eve_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.event                   ADD PRIMARY KEY USING INDEX event_pk;

CREATE UNIQUE INDEX event_handler_pk ON darts.event_handler(evh_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.event_handler              ADD PRIMARY KEY USING INDEX event_handler_pk;

CREATE UNIQUE INDEX external_object_directory_pk ON darts.external_object_directory(eod_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.external_object_directory   ADD PRIMARY KEY USING INDEX external_object_directory_pk;

CREATE UNIQUE INDEX external_location_type_pk ON darts.external_location_type(elt_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.external_location_type   ADD PRIMARY KEY USING INDEX external_location_type_pk;

CREATE UNIQUE INDEX hearing_pk ON darts.hearing(hea_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.hearing                 ADD PRIMARY KEY USING INDEX hearing_pk;

CREATE UNIQUE INDEX hearing_event_ae_pk ON darts.hearing_event_ae(hev_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.hearing_event_ae        ADD PRIMARY KEY USING INDEX hearing_event_ae_pk;

CREATE UNIQUE INDEX hearing_media_ae_pk ON darts.hearing_media_ae(hma_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.hearing_media_ae        ADD PRIMARY KEY USING INDEX hearing_media_ae_pk;

CREATE UNIQUE INDEX media_pk ON darts.media(med_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.media                   ADD PRIMARY KEY USING INDEX media_pk;

CREATE UNIQUE INDEX media_request_pk ON darts.media_request(mer_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.media_request           ADD PRIMARY KEY USING INDEX media_request_pk;

CREATE UNIQUE INDEX notification_pk ON darts.notification(not_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.notification            ADD PRIMARY KEY USING INDEX notification_pk;

CREATE UNIQUE INDEX object_directory_status_pk ON darts.object_directory_status(ods_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.object_directory_status ADD PRIMARY KEY USING INDEX object_directory_status_pk;

CREATE UNIQUE INDEX region_pk ON darts.region(reg_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.region                  ADD PRIMARY KEY USING INDEX region_pk;

CREATE UNIQUE INDEX report_pk ON darts.report(rep_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.report                  ADD PRIMARY KEY USING INDEX report_pk;

CREATE UNIQUE INDEX reporting_restrictons_pk ON darts.reporting_restrictions(rer_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.reporting_restrictions  ADD PRIMARY KEY USING INDEX reporting_restrictons_pk;

CREATE UNIQUE INDEX transcription_pk ON darts.transcription(tra_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.transcription           ADD PRIMARY KEY USING INDEX transcription_pk;

CREATE UNIQUE INDEX transcription_comment_pk ON darts.transcription_comment(trc_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.transcription_comment   ADD PRIMARY KEY USING INDEX transcription_comment_pk;

CREATE UNIQUE INDEX transcription_type_pk ON darts.transcription_type(trt_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.transcription_type      ADD PRIMARY KEY USING INDEX transcription_type_pk;

CREATE UNIQUE INDEX transient_object_directory_pk ON darts.transient_object_directory(tod_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.transient_object_directory  ADD PRIMARY KEY USING INDEX transient_object_directory_pk;

CREATE UNIQUE INDEX urgency_pk ON darts.urgency(urg_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.urgency                 ADD PRIMARY KEY USING INDEX urgency_pk;

CREATE UNIQUE INDEX user_account_pk ON darts.user_account( usr_id) TABLESPACE darts_indexes;
ALTER  TABLE darts.user_account            ADD PRIMARY KEY USING INDEX user_account_pk;

-- defaults for postgres sequences, datatype->bigint, increment->1, nocycle is default, owned by none
CREATE SEQUENCE darts.ann_seq CACHE 20;
CREATE SEQUENCE darts.cas_seq CACHE 20;
CREATE SEQUENCE darts.cth_seq CACHE 20;
CREATE SEQUENCE darts.cra_seq CACHE 20;
CREATE SEQUENCE darts.ctr_seq CACHE 20;
CREATE SEQUENCE darts.dal_seq CACHE 20;
CREATE SEQUENCE darts.der_seq CACHE 20;
CREATE SEQUENCE darts.eve_seq CACHE 20;
CREATE SEQUENCE darts.evh_seq CACHE 20;
CREATE SEQUENCE darts.eod_seq CACHE 20;
CREATE SEQUENCE darts.elt_seq CACHE 20;
CREATE SEQUENCE darts.hea_seq CACHE 20;
CREATE SEQUENCE darts.hev_seq CACHE 20;
CREATE SEQUENCE darts.hma_seq CACHE 20;
CREATE SEQUENCE darts.med_seq CACHE 20;
CREATE SEQUENCE darts.mer_seq CACHE 20;
CREATE SEQUENCE darts.not_seq CACHE 20;
CREATE SEQUENCE darts.ods_seq CACHE 20;
CREATE SEQUENCE darts.reg_seq CACHE 20;
CREATE SEQUENCE darts.rep_seq CACHE 20;
CREATE SEQUENCE darts.rer_seq CACHE 20;
CREATE SEQUENCE darts.tra_seq CACHE 20;
CREATE SEQUENCE darts.trc_seq CACHE 20;
CREATE SEQUENCE darts.trt_seq CACHE 20;
CREATE SEQUENCE darts.tod_seq CACHE 20;
CREATE SEQUENCE darts.urg_seq CACHE 20;
CREATE SEQUENCE darts.usr_seq CACHE 20;

-- foreign keys

ALTER TABLE darts.annotation
ADD CONSTRAINT annotation_case_fk
FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.annotation
ADD CONSTRAINT annotation_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.court_case
ADD CONSTRAINT court_case_reporting_restriction_fk
FOREIGN KEY (rer_id) REFERENCES darts.reporting_restrictions(rer_id);

ALTER TABLE darts.court_case
ADD CONSTRAINT court_case_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.courthouse_region_ae
ADD CONSTRAINT courthouse__region_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.courthouse_region_ae
ADD CONSTRAINT courthouse__region_region_fk
FOREIGN KEY (reg_id) REFERENCES darts.region(reg_id);

ALTER TABLE darts.hearing
ADD CONSTRAINT hearing_case_fk
FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.courtroom
ADD CONSTRAINT courtroom_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.daily_list
ADD CONSTRAINT daily_list_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.device_register
ADD CONSTRAINT device_register_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.event
ADD CONSTRAINT event_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.event
ADD CONSTRAINT event_event_handler_fk
FOREIGN KEY (evh_id) REFERENCES darts.event_handler(evh_id);

ALTER TABLE darts.external_object_directory
ADD CONSTRAINT eod_media_fk
FOREIGN KEY (med_id) REFERENCES darts.media(med_id);

ALTER TABLE darts.external_object_directory
ADD CONSTRAINT eod_transcription_fk
FOREIGN KEY (tra_id) REFERENCES darts.transcription(tra_id);

ALTER TABLE darts.external_object_directory
ADD CONSTRAINT eod_annotation_fk
FOREIGN KEY (ann_id) REFERENCES darts.annotation(ann_id);

ALTER TABLE darts.external_object_directory
ADD CONSTRAINT eod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.external_object_directory
ADD CONSTRAINT eod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES darts.object_directory_status(ods_id);

ALTER TABLE darts.external_object_directory
ADD CONSTRAINT eod_external_location_type_fk
FOREIGN KEY (elt_id) REFERENCES darts.external_location_type(elt_id);

ALTER TABLE darts.hearing
ADD CONSTRAINT hearing_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.hearing_event_ae
ADD CONSTRAINT hearing_event_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES darts.hearing(hea_id);

ALTER TABLE darts.hearing_event_ae
ADD CONSTRAINT hearing_event_ae_event_fk
FOREIGN KEY (eve_id) REFERENCES darts.event(eve_id);

ALTER TABLE darts.hearing_media_ae
ADD CONSTRAINT hearing_media_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES darts.hearing(hea_id);

ALTER TABLE darts.hearing_media_ae
ADD CONSTRAINT hearing_media_ae_media_fk
FOREIGN KEY (med_id) REFERENCES darts.media(med_id);

ALTER TABLE darts.media
ADD CONSTRAINT media_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.media_request
ADD CONSTRAINT media_hearing_fk
FOREIGN KEY (hea_id) REFERENCES darts.hearing(hea_id);

ALTER TABLE darts.notification
ADD CONSTRAINT notification_case_fk
FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_case_fk
FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_urgency_fk
FOREIGN KEY (urg_id) REFERENCES darts.urgency(urg_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_requested_by_fk
FOREIGN KEY (requested_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_approved_by_fk
FOREIGN KEY (approved_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_transcribed_by_fk
FOREIGN KEY (transcribed_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
ADD CONSTRAINT transcription_transcription_type_fk
FOREIGN KEY (trt_id) REFERENCES darts.transcription_type(trt_id);

ALTER TABLE darts.transcription_comment
ADD CONSTRAINT transcription_comment_transcription_fk
FOREIGN KEY (tra_id) REFERENCES darts.transcription(tra_id);

ALTER TABLE darts.transcription_comment
ADD CONSTRAINT transcription_comment_author_fk
FOREIGN KEY (author) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transient_object_directory
ADD CONSTRAINT tod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transient_object_directory
ADD CONSTRAINT tod_media_request_fk
FOREIGN KEY (mer_id) REFERENCES darts.media_request(mer_id);

ALTER TABLE darts.transient_object_directory
ADD CONSTRAINT tod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES darts.object_directory_status(ods_id);

-- additional unique multi-column indexes and constraints

--,UNIQUE (cth_id,courtroom_name)
CREATE UNIQUE INDEX ctr_chr_crn_unq ON darts.courtroom( cth_id, courtroom_name) TABLESPACE darts_indexes;
ALTER  TABLE darts.courtroom ADD UNIQUE USING INDEX ctr_chr_crn_unq;

--,UNIQUE(cas_id,ctr_id,c_hearing_date)
CREATE UNIQUE INDEX hea_cas_ctr_hd_unq ON darts.hearing( cas_id, ctr_id,hearing_date) TABLESPACE darts_indexes;
ALTER  TABLE darts.hearing ADD UNIQUE USING INDEX hea_cas_ctr_hd_unq;

--,UNIQUE(cth_id, case_number)
CREATE UNIQUE INDEX cas_case_number_cth_id_unq ON darts.court_case(case_number,cth_id) TABLESPACE darts_indexes;
ALTER TABLE darts.court_case ADD UNIQUE USING INDEX cas_case_number_cth_id_unq;

INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'New');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Stored');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Failure');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Failure - File not found');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Failure - File size check failed');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Failure - File type check failed');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Failure - Checksum failed');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Failure - ARM ingestion failed');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Awaiting Verification');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'marked for Deletion');
INSERT INTO darts.object_directory_status (ods_id,ods_description) VALUES (nextval('darts.ods_seq'),'Deleted');

INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (1,'Section 39 of the Children and Young Persons Act 1999');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (2,'An order made under s45 of the Youth Justice and Criminal Evidence Act 1999');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (3,'Section 4(2) of the Contempt of Court Act 1981');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (4,'Restrictions Lifted');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (5,'Section 2 of the Sexual Offenders (Amendment) Act 1992');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (6,'Section 11 of the Contempt of Court Act 1981');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (7,'An order made under s45a of the Youth Justice and Criminal Evidence Act 1999');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (8,'An order made under s46 of the Youth Justice and Criminal Evidence Act 1999');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES  (9,'Section 4 of the Sexual Offenders (Amendment) Act 1976');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES (10,'Judge directed on reporting restrictions');
INSERT INTO darts.reporting_restrictions (rer_id,rer_description) VALUES (11,'An order made under s49 of the Children and Young Persons Act 1933');








