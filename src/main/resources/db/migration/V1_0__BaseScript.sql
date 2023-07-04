--v6 add sequences,remove character/numeric size limits, change DATE to TIMESTAMP
--v7 consistently add the legacy primary and foreign keys
--v8 3NF courthouses
--v9 remove various legacy columns
--v10 change some numeric columns to boolean, remove unused legacy column c_upload_priority
--v11 introduce many:many case:hearing, removed version label & superceded from hearing, as no source for migration, and assume unneeded by modernised
--v12 remove reporting_restrictions from annotation,cached_media,event,media,transcription,transformation_request
--    add message_id, event_type_id to event
--    add event_type table and links to event by FK
--v13 adding Not null to transcription FK cas_id & crt_id,
--    adding transcription_type table to replace c_type. Remove fields c_notification_type, c_urgent from transcription
--    add event_name to event
--    add urgency table and fk to transcription
--    added comment_ts and author to transcription_comment
--v14 removing unneeded columns from courthouse, normalising crown court code from daily list
--    amending judge,defendant,defence, prosecutor on hearing to be 1-d array instead of scalar
--    rename i_version_label to i_version
--v15 remove crt_id from case and corresponding FK
--v16 add hea_id to transcription and corresponding FK
--    add user to this script
--v17 further comments reagrding properties of live data
--v18 moving atributes from hearing to case, changing timestamps to ts with tz
--v19 amended courthouse_name to be unique, amended courthouse_code to be integer
--    removing c_scheduled_start from case, to be replaced by 2 columns on hearing, scheduled_start_time and hearing_is_actual flag
--v20 moving event and media to link to hearing rather than case, resulting in case_event_ae and
--    case_media_ae changing name
--v21 normalising c_reporting_restrictions into reporting_restriction table
--    change alias for courthouse from CRT to CTH, accommodate new COURTROOM table aliased to CTR
--    add COURTROOM table, replace existing FKs to COURTHOUSE with ones to COURTROOM for event, media
--    rename event_type.type to .evt_type
--    remove c_courtroom from annotation,,cached_media, event, hearing, media,
--    transcription, transformation_request
--    Remove associative entity case_hearing, replace with simple PK-FK relation
--v22 updated all sequences to cache 20
--    updated daily_list and introduced notification
--v23 remove transformation_request, transformation_log, cached_media, replace with media_request
--v24 adding request_type to media_request omitted in error
--    amending external_object_directory to store one external address per record
--    amended c_case_id to c_case_number
--    replacing all smallint with integer, which includes all use of i_version
--    courthouse.courthouse_code, media_request.req_proc_attempts, notification.send_attempts
--    remove i_version, r_version_label and i_version_label from case, due to no legacy versioned data
--    amend daily_list content column to be character varying from xml, to store list in JSON format
--    remove c_type from case
--v25 adding tablespace clauses to tables and indexes
--v26 adding multi-column unique constraints to courtroom and hearing
--v27 adding c_case_id to event, adding transfer_attempts to both object_directory Tables
--    changing checksum from uuid to character varying
--v28 removing all  prefixes to table and pk columns
--    reinstating cth_id to case table



-- List of Table Aliases
-- annotation                 ANN
-- case                       CAS
-- courthouse                 CTH
-- courtroom                  CTR
-- daily_list                 DAL
-- event                      EVE
-- event_type                 EVT
-- external_object_directory      EOD
-- hearing                    HEA
-- hearing_event_ae           HEV
-- hearing_media_ae           HMA
-- media                      MED
-- media_request              MER
-- notification               NOT
-- object_directory_status    ODS
-- report                     REP
-- reporting_restrictions     RER
-- transcription              TRA
-- transcription_type         TRT
-- transient_object_directory TOD
-- urgency                    URG
-- user                       USR

--CREATE SCHEMA DARTS;


CREATE TABLE darts.annotation
(ann_id                   INTEGER					 NOT NULL
,cas_id                   INTEGER					 NOT NULL
,ctr_id                   INTEGER
,r_annotation_object_id   CHARACTER VARYING(16)
,c_text                   CHARACTER VARYING
,c_time_stamp             TIMESTAMP WITH TIME ZONE
,c_start                  TIMESTAMP WITH TIME ZONE
,c_end                    TIMESTAMP WITH TIME ZONE
,c_case_id                CHARACTER VARYING(32)                     --this is a placeholder for case_document_r.c_case_id, known to be singular for annotation object types
,r_case_object_id         CHARACTER VARYING(16)	                    --this is a placeholder for case_s.r_object_id, and must be derived from case_document_r.c_case_id
,r_version_label          CHARACTER VARYING(32)
,i_superseded             BOOLEAN
,i_version                INTEGER
);

COMMENT ON COLUMN darts.annotation.ann_id
IS 'primary key of annotation';

COMMENT ON COLUMN darts.annotation.cas_id
IS 'foreign key from court_case';

COMMENT ON COLUMN darts.annotation.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.annotation.r_annotation_object_id
IS 'internal Documentum primary key from annotation_s';

COMMENT ON COLUMN darts.annotation.c_text
IS 'directly sourced from annotation_s';

COMMENT ON COLUMN darts.annotation.c_time_stamp
IS 'directly sourced from annotation_s';

COMMENT ON COLUMN darts.annotation.c_start
IS 'inherited from case_document_s';

COMMENT ON COLUMN darts.annotation.c_end
IS 'inherited from case_document_s';

COMMENT ON COLUMN darts.annotation.r_case_object_id
IS 'internal Documentum id from case_s acting as foreign key';

COMMENT ON COLUMN darts.annotation.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of annotation';

CREATE TABLE darts.court_case
(cas_id                    INTEGER					 NOT NULL
,cth_id                    INTEGER                   NOT NULL
,rer_id                    INTEGER
,r_case_object_id          CHARACTER VARYING(16)
,c_case_number             CHARACTER VARYING     -- maps to c_case_id in legacy
,c_closed                  BOOLEAN
,c_interpreter_used        BOOLEAN
,c_case_closed_ts          TIMESTAMP WITH TIME ZONE
,c_defendant               CHARACTER VARYING[]
,c_prosecutor              CHARACTER VARYING[]
,c_defence                 CHARACTER VARYING[]
,retain_until_ts           TIMESTAMP WITH TIME ZONE
,r_version_label           CHARACTER VARYING(32)
,i_superseded              BOOLEAN
,i_version                 INTEGER
);

COMMENT ON COLUMN darts.court_case.cas_id
IS 'primary key of court_case';

COMMENT ON COLUMN darts.court_case.cth_id
IS 'foreign key to courthouse';

COMMENT ON COLUMN darts.court_case.rer_id
IS 'foreign key to reporting_restrictions';

COMMENT ON COLUMN darts.court_case.r_case_object_id
IS 'internal Documentum primary key from case_s';

COMMENT ON COLUMN darts.court_case.c_case_number
IS 'directly sourced from case_s.c_case_id';

COMMENT ON COLUMN darts.court_case.c_closed
IS 'migrated from case_s, converted from numeric to boolean';

COMMENT ON COLUMN darts.court_case.c_interpreter_used
IS 'migrated from case_s, converted from numeric to boolean';

COMMENT ON COLUMN darts.court_case.c_case_closed_ts
IS 'directly sourced from case_s.c_case_closed_date';

COMMENT ON COLUMN darts.court_case.c_defendant
IS 'directly sourced from case_r';

COMMENT ON COLUMN darts.court_case.c_prosecutor
IS 'directly sourced from case_r';

COMMENT ON COLUMN darts.court_case.c_defence
IS 'directly sourced from case_r';

COMMENT ON COLUMN darts.court_case.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of case, containing the version record';

COMMENT ON COLUMN darts.court_case.i_version
IS 'inherited from dm_sysobject_r, for r_object_type of case, set according to the current flag';

CREATE TABLE darts.courthouse
(cth_id                    INTEGER					 NOT NULL
,courthouse_code           INTEGER                                     UNIQUE
,courthouse_name           CHARACTER VARYING         NOT NULL          UNIQUE
,created_ts                TIMESTAMP WITH TIME ZONE  NOT NULL
,last_modified_ts          TIMESTAMP WITH TIME ZONE  NOT NULL
);

COMMENT ON COLUMN darts.courthouse.cth_id
IS 'primary key of courthouse';

COMMENT ON COLUMN darts.courthouse.courthouse_code
IS 'corresponds to the c_crown_court_code found in daily lists';

COMMENT ON COLUMN darts.courthouse.courthouse_name
IS 'directly sourced from courthouse_s.c_id';

CREATE TABLE darts.courtroom
(ctr_id                     INTEGER                  NOT NULL
,cth_id                     INTEGER                  NOT NULL
,courtroom_name             CHARACTER VARYING        NOT NULL
--,UNIQUE(cth_id,courtroom_name)
);

COMMENT ON COLUMN darts.courtroom.ctr_id
IS 'primary key of courtroom';

COMMENT ON COLUMN darts.courtroom.cth_id
IS 'foreign key to courthouse';

CREATE TABLE darts.daily_list
(dal_id                     INTEGER					 NOT NULL
,cth_id                     INTEGER					 NOT NULL
,r_daily_list_object_id     CHARACTER VARYING(16)
,c_unique_id                CHARACTER VARYING
--,c_crown_court_name       CHARACTER VARYING        -- removed, normalised to courthouses, but note that in legacy there is mismatch between courthouse_s.c_id and daily_list_s.c_crown_court_name to be resolved
,c_job_status               CHARACTER VARYING        -- one of "New","Partially Processed","Processed","Ignored","Invalid"
,c_published_time           TIMESTAMP WITH TIME ZONE
,c_daily_list_id            NUMERIC                  -- all 0
,c_start_ts                 TIMESTAMP WITH TIME ZONE
,c_end_ts                   TIMESTAMP WITH TIME ZONE -- all values match c_start_date
,c_daily_list_id_s          CHARACTER VARYING        -- non unique integer in legacy
,c_daily_list_source        CHARACTER VARYING        -- one of CPP,XHB ( live also sees nulls and spaces)
,daily_list_content         CHARACTER VARYING
,created_ts                 TIMESTAMP WITH TIME ZONE
,last_modified_ts           TIMESTAMP WITH TIME ZONE
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version                  INTEGER
);

COMMENT ON COLUMN darts.daily_list.dal_id
IS 'primary key of daily_list';

COMMENT ON COLUMN darts.daily_list.cth_id
IS 'foreign key from courthouse';

COMMENT ON COLUMN darts.daily_list.r_daily_list_object_id
IS 'internal Documentum primary key from daily_list_s';

COMMENT ON COLUMN darts.daily_list.c_unique_id
IS 'directly sourced from daily_list_s, received as part of the XML, used to find duplicate daily lists';

COMMENT ON COLUMN darts.daily_list.c_job_status
IS 'directly sourced from daily_list_s';

COMMENT ON COLUMN darts.daily_list.c_published_time
IS 'directly sourced from daily_list_s.c_timestamp';

COMMENT ON COLUMN darts.daily_list.c_daily_list_id
IS 'directly sourced from daily_list_s';

COMMENT ON COLUMN darts.daily_list.c_start_ts
IS 'directly sourced from daily_list_s.c_start_date';

COMMENT ON COLUMN darts.daily_list.c_end_ts
IS 'directly sourced from daily_list_s.c_end_date';

COMMENT ON COLUMN darts.daily_list.c_daily_list_id_s
IS 'directly sourced from daily_list_s';

COMMENT ON COLUMN darts.daily_list.c_daily_list_source
IS 'directly sourced from daily_list_s';

COMMENT ON COLUMN darts.daily_list.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of daily_list';

CREATE TABLE darts.event
(eve_id                     INTEGER					 NOT NULL
,ctr_id                     INTEGER
,evt_id                     INTEGER
,r_event_object_id          CHARACTER VARYING(16)
,c_event_id                 NUMERIC
,event_name                 CHARACTER VARYING
,event_text                 CHARACTER VARYING
,c_time_stamp               TIMESTAMP WITH TIME ZONE
,c_case_id                  CHARACTER VARYING(32)[]
,r_version_label            CHARACTER VARYING(32)
,message_id                 CHARACTER VARYING
,i_superseded               BOOLEAN
,i_version                  INTEGER
);

COMMENT ON COLUMN darts.event.eve_id
IS 'primary key of event';

COMMENT ON COLUMN darts.event.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.event.evt_id
IS 'foreign key for the event_type table';

COMMENT ON COLUMN darts.event.r_event_object_id
IS 'internal Documentum primary key from event_s';

COMMENT ON COLUMN darts.event.c_event_id
IS 'directly sourced from event_s';

COMMENT ON COLUMN darts.event.event_name
IS 'inherited from dm_sysobect_s.object_name';

COMMENT ON COLUMN darts.event.event_text
IS 'inherited from annotation_s.c_text';

COMMENT ON COLUMN darts.event.c_time_stamp
IS 'inherited from annotation_s';

COMMENT ON COLUMN darts.event.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of event';

COMMENT ON COLUMN darts.event.message_id
IS 'no migration element, records the id of the message that gave rise to this event';


CREATE TABLE darts.event_type
(evt_id                      INTEGER					 NOT NULL
,evt_type                    CHARACTER VARYING           NOT NULL
,sub_type                    CHARACTER VARYING
,event_name                  CHARACTER VARYING           NOT NULL
,handler                     CHARACTER VARYING
);

COMMENT ON TABLE darts.event_type
IS 'content will be derived from TBL_DOC_HANDLER in the legacy database, but currently has no primary key and 6 fully duplicated rows';

COMMENT ON COLUMN darts.event_type.evt_id
IS 'primary key of event_type';

COMMENT ON COLUMN darts.event_type.evt_type
IS 'directly sourced from doc_type';

COMMENT ON COLUMN darts.event_type.sub_type
IS 'directly sourced from doc_sub_type';

COMMENT ON COLUMN darts.event_type.event_name
IS 'directly sourced from event_name';

COMMENT ON COLUMN darts.event_type.handler
IS 'directly sourced from doc_handler';


CREATE TABLE darts.external_object_directory
(eod_id                      INTEGER			 		 NOT NULL
,med_id                      INTEGER
,tra_id                      INTEGER
,ann_id                      INTEGER
,ods_id                      INTEGER           -- FK to object_directory_status
-- additional optional FKs to other relevant internal objects would require columns here
,external_location           UUID
,external_location_type      CHARACTER VARYING -- one of inbound,unstructured,arm,tempstore,vodafone
,checksum	                 CHARACTER VARYING
,transfer_attempts           INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE
,modified_ts                 TIMESTAMP WITH TIME ZONE
,modified_by                 INTEGER           -- FK to user.usr_id
);

COMMENT ON COLUMN darts.external_object_directory.eod_id
IS 'primary key of external_object_directory';

-- added two foreign key columns, but there will be as many FKs as there are distinct objects with externally stored components

COMMENT ON COLUMN darts.external_object_directory.med_id
IS 'foreign key from media';

COMMENT ON COLUMN darts.external_object_directory.tra_id
IS 'foreign key from transcription';

CREATE TABLE darts.hearing
(hea_id                     INTEGER					   NOT NULL
,cas_id                     INTEGER                    NOT NULL
,ctr_id                     INTEGER                    NOT NULL
,c_judge                    CHARACTER VARYING[]
,c_hearing_date             DATE     -- to record only DATE component of hearings, both scheduled and actual
,c_scheduled_start_time     TIME     -- to record only TIME component of hearings, while they are scheduled only
,hearing_is_actual          BOOLEAN  -- TRUE for actual hearings, FALSE for scheduled hearings
,c_judge_hearing_date       CHARACTER VARYING
--,UNIQUE(cas_id,ctr,c_hearing_date)
);

COMMENT ON COLUMN darts.hearing.hea_id
IS 'primary key of hearing';

COMMENT ON COLUMN darts.hearing.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN darts.hearing.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.hearing.c_judge
IS 'directly sourced from case_r';

COMMENT ON COLUMN darts.hearing.c_hearing_date
IS 'directly sourced from case_r';

COMMENT ON COLUMN darts.hearing.c_judge_hearing_date
IS 'directly sourced from case_r';

CREATE TABLE darts.hearing_event_ae
(hev_id                     INTEGER							 NOT NULL
,hea_id                     INTEGER							 NOT NULL
,eve_id                     INTEGER							 NOT NULL
);

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
);

COMMENT ON COLUMN darts.hearing_media_ae.hma_id
IS 'primary key of hearing_media_ae';

COMMENT ON COLUMN darts.hearing_media_ae.hea_id
IS 'foreign key from case, part of composite natural key';

COMMENT ON COLUMN darts.hearing_media_ae.med_id
IS 'foreign key from media, part of composite natural key';

CREATE TABLE darts.media
(med_id                     INTEGER					 NOT NULL
,ctr_id                     INTEGER
,media_id                   INTEGER
,r_media_object_id          CHARACTER VARYING(16)
,c_channel                  NUMERIC
,c_total_channels           NUMERIC                  --99.9% are "4" in legacy
,c_reference_id             CHARACTER VARYING        --all nulls in legacy
,c_start                    TIMESTAMP WITH TIME ZONE
,c_end                      TIMESTAMP WITH TIME ZONE
,c_case_id                  CHARACTER VARYING(32)[]  --this is a placeholder for case_document_r.c_case_id, known to be repeated for media object types
,r_case_object_id           CHARACTER VARYING(16)[]  --this is a placeholder for case_s.r_object_id, and must be derived from case_document_r.c_case_id
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version                  INTEGER
);

COMMENT ON COLUMN darts.media.med_id
IS 'primary key of media';

COMMENT ON COLUMN darts.media.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.media.r_media_object_id
IS 'internal Documentum primary key from media_s';

COMMENT ON COLUMN darts.media.c_channel
IS 'directly sourced from media_s';

COMMENT ON COLUMN darts.media.c_total_channels
IS 'directly sourced from media_s';

COMMENT ON COLUMN darts.media.c_reference_id
IS 'directly sourced from media_s';

COMMENT ON COLUMN darts.media.c_start
IS 'inherited from case_document_s';

COMMENT ON COLUMN darts.media.c_end
IS 'inherited from case_document_s';

COMMENT ON COLUMN darts.media.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of media';

CREATE TABLE darts.media_request
(mer_id                     INTEGER                    NOT NULL
,hea_id                     INTEGER                    NOT NULL
,requestor                  INTEGER                    NOT NULL  -- FK to user.usr_id
,request_status             CHARACTER VARYING
,request_type               CHARACTER VARYING
,req_proc_attempts          INTEGER
,start_ts                   TIMESTAMP WITH TIME ZONE
,end_ts                     TIMESTAMP WITH TIME ZONE
,created_ts                 TIMESTAMP WITH TIME ZONE
,last_updated_ts            TIMESTAMP WITH TIME ZONE
,last_accessed_ts           TIMESTAMP WITH TIME ZONE
,output_filename            CHARACTER VARYING
,output_format              CHARACTER VARYING
);

COMMENT ON COLUMN darts.media_request.mer_id
IS 'primary key of media_request';

COMMENT ON COLUMN darts.media_request.hea_id
IS 'foreign key of hearing';

COMMENT ON COLUMN darts.media_request.requestor
IS 'requestor of the media request, possibly migrated from transformation_request_s';

COMMENT ON COLUMN darts.media_request.request_status
IS 'status of the migration request';

COMMENT ON COLUMN darts.media_request.req_proc_attempts
IS 'number of attempts by ATS to process the request';

COMMENT ON COLUMN darts.media_request.start_ts
IS 'start time in the search criteria for request, possibly migrated from cached_media_s or transformation_request_s';

COMMENT ON COLUMN darts.media_request.end_ts
IS 'end time in the search criteria for request, possibly migrated from cached_media_s or transformation_request_s';

COMMENT ON COLUMN darts.media_request.output_filename
IS 'filename of the requested media object, possibly migrated from transformation_request_s';

COMMENT ON COLUMN darts.media_request.output_format
IS 'format of the requested media object, possibly migrated from transformation_s';



CREATE TABLE darts.notification
(not_id                     INTEGER                    NOT NULL
,cas_id                     INTEGER                    NOT NULL
,notification_event   		CHARACTER VARYING          NOT NULL
,notification_status        CHARACTER VARYING          NOT NULL
,email_address              CHARACTER VARYING          NOT NULL
,send_attempts              INTEGER
,template_values            CHARACTER VARYING
,created_ts                 TIMESTAMP WITH TIME ZONE   NOT NULL
,last_updated_ts            TIMESTAMP WITH TIME ZONE   NOT NULL
);

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
(ods_id                     INTEGER
,ods_description            CHARACTER VARYING
);

COMMENT ON TABLE darts.object_directory_status
IS 'used to record acceptable statuses found in [external/transient]_object_directory';


CREATE TABLE darts.report
(rep_id                     INTEGER					 NOT NULL
,r_report_object_id         CHARACTER VARYING(16)
,c_name                     CHARACTER VARYING
,c_subject                  CHARACTER VARYING
,c_text                     CHARACTER VARYING
,c_query                    CHARACTER VARYING
,c_recipients               CHARACTER VARYING
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version                  INTEGER
);

COMMENT ON COLUMN darts.report.rep_id
IS 'primary key of report';

COMMENT ON COLUMN darts.report.r_report_object_id
IS 'internal Documentum primary key from report_s';

COMMENT ON COLUMN darts.report.c_name
IS 'directly sourced from report_s';

COMMENT ON COLUMN darts.report.c_subject
IS 'directly sourced from report_s';

COMMENT ON COLUMN darts.report.c_text
IS 'directly sourced from report_s';

COMMENT ON COLUMN darts.report.c_query
IS 'directly sourced from report_s';

COMMENT ON COLUMN darts.report.c_recipients
IS 'directly sourced from report_s';

COMMENT ON COLUMN darts.report.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of report';

CREATE TABLE darts.reporting_restrictions
(rer_id                     INTEGER                  NOT NULL
,rer_description            CHARACTER VARYING
);

COMMENT ON COLUMN darts.reporting_restrictions.rer_id
IS 'primary key of reporting_restrictions';

COMMENT ON COLUMN darts.reporting_restrictions.rer_description
IS 'text of the relevant legislation, to be populated from case_s.c_reporting_restrictions';

CREATE TABLE darts.transcription
(tra_id                     INTEGER					 NOT NULL
,cas_id                     INTEGER                  NOT NULL
,ctr_id                     INTEGER                  NOT NULL
,trt_id                     INTEGER                  NOT NULL
,urg_id                     INTEGER                  -- remains nullable, as nulls present in source data ( c_urgency)
,hea_id                     INTEGER                  -- remains nullable, until migration is complete
,r_transcription_object_id  CHARACTER VARYING(16)    -- legacy pk from transcription_s.r_object_id
,c_company                  CHARACTER VARYING        -- effectively unused in legacy, either null or "<this field will be completed by the system>"
,c_requestor                CHARACTER VARYING        -- 1055 distinct, from <forname><surname> to <AAANNA>
,c_current_state            CHARACTER VARYING        -- 23 distinct, far more than 5 expected (requested,awaiting authorisation,with transcribed, complete, rejected)
,i_current_state_ts         TIMESTAMP WITH TIME ZONE -- date & time record entered the current c_current_state
,c_hearing_date             TIMESTAMP WITH TIME ZONE -- 3k records have time component, but all times are 23:00,so effectively DATE only, will be absolete once hea_id populated
,c_start                    TIMESTAMP WITH TIME ZONE -- both c_start and c_end have time components
,c_end                      TIMESTAMP WITH TIME ZONE -- we have 49k rows in legacy transcription_s, 7k have c_end != c_start
,created_ts                 TIMESTAMP WITH TIME ZONE
,last_modified_ts           TIMESTAMP WITH TIME ZONE
,last_modified_by           INTEGER                  -- will need to be FK to users table
,requested_by               INTEGER                  -- will need to be FK to users table
,approved_by                INTEGER                  -- will need to be FK to users table
,approved_on_ts             TIMESTAMP WITH TIME ZONE
,transcribed_by             INTEGER                  -- will need to be FK to users table
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version                  INTEGER
);

COMMENT ON COLUMN darts.transcription.tra_id
IS 'primary key of transcription';

COMMENT ON COLUMN darts.transcription.cas_id
IS 'foreign key from case';

COMMENT ON COLUMN darts.transcription.ctr_id
IS 'foreign key from courtroom';

COMMENT ON COLUMN darts.transcription.urg_id
IS 'foreign key from urgency';

COMMENT ON COLUMN darts.transcription.trt_id
IS 'foreign key to transcription_type, sourced from transcription_s.c_type';

COMMENT ON COLUMN darts.transcription.r_transcription_object_id
IS 'internal Documentum primary key from transcription_s';

COMMENT ON COLUMN darts.transcription.c_company
IS 'directly sourced from transcription_s';

COMMENT ON COLUMN darts.transcription.c_requestor
IS 'directly sourced from transcription_s';

COMMENT ON COLUMN darts.transcription.c_current_state
IS 'directly sourced from transcription_s';

COMMENT ON COLUMN darts.transcription.c_hearing_date
IS 'directly sourced from transcription_s';

COMMENT ON COLUMN darts.transcription.c_start
IS 'inherited from case_document_s';

COMMENT ON COLUMN darts.transcription.c_end
IS 'inherited from case_document_s';

COMMENT ON COLUMN darts.transcription.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of transcription';

CREATE TABLE darts.transcription_comment
(trc_id                            INTEGER					 NOT NULL
,tra_id                            INTEGER
,r_transcription_object_id         CHARACTER VARYING(16)     -- this is a placeholder for transcription_s.r_object_id
,c_comment                         CHARACTER VARYING
,comment_ts                        TIMESTAMP WITH TIME ZONE
,author                            INTEGER                   -- will need to be FK to user table
,created_ts                        TIMESTAMP WITH TIME ZONE
,last_modified_ts                  TIMESTAMP WITH TIME ZONE
,last_modified_by                  INTEGER                   -- will need to be FK to users table
,i_superseded                      BOOLEAN
,i_version                         INTEGER
);

COMMENT ON COLUMN darts.transcription_comment.trc_id
IS 'primary key of transcription_comment';

COMMENT ON COLUMN darts.transcription_comment.tra_id
IS 'foreign key from transcription';

COMMENT ON COLUMN darts.transcription_comment.r_transcription_object_id
IS 'internal Documentum primary key from transcription_s';

COMMENT ON COLUMN darts.transcription_comment.c_comment
IS 'directly sourced from transcription_r';

COMMENT ON COLUMN darts.transcription_comment.r_transcription_object_id
IS 'internal Documentum id from transcription_s acting as foreign key';

CREATE TABLE darts.transcription_type
(trt_id                            INTEGER                   NOT NULL
,description                       CHARACTER VARYING
);

COMMENT ON TABLE darts.transcription_type
IS 'standing data table, migrated from tbl_transcription_type';

COMMENT ON COLUMN darts.transcription_type.trt_id
IS 'primary key, but not sequence generated';

CREATE TABLE darts.transient_object_directory
(tod_id                      INTEGER			 		 NOT NULL
,mer_id                      INTEGER
,ods_id                      INTEGER           -- FK to object_directory_status.ods_id
,external_location           UUID
,checksum	                 CHARACTER VARYING
,transfer_attempts           INTEGER
,created_ts                  TIMESTAMP WITH TIME ZONE
,modified_ts                 TIMESTAMP WITH TIME ZONE
,modified_by                 INTEGER           -- FK to user.usr_id
);

CREATE TABLE darts.urgency
(urg_id                            INTEGER                 NOT NULL
,description                       CHARACTER VARYING
);

COMMENT ON TABLE darts.urgency
IS 'will be migrated from tbl_urgency';

COMMENT ON COLUMN darts.urgency.urg_id
IS 'inherited from tbl_urgency.urgency_id';

COMMENT ON COLUMN darts.urgency.description
IS 'inherited from tbl_urgency.description';

CREATE TABLE darts.user_account
(usr_id                  INTEGER
,r_dm_user_s_object_id   CHARACTER VARYING(16)
,user_name               CHARACTER VARYING
,user_os_name            CHARACTER VARYING
,user_address            CHARACTER VARYING
,user_privileges         NUMERIC
,user_db_name            CHARACTER VARYING
,description             CHARACTER VARYING
,user_state              NUMERIC
,r_modify_date           TIMESTAMP WITH TIME ZONE
,workflow_disabled       NUMERIC
,user_source             CHARACTER VARYING
,user_ldap_cn            CHARACTER VARYING
,user_global_unique_id   CHARACTER VARYING
,user_login_name         CHARACTER VARYING
,user_login_domain       CHARACTER VARYING
,last_login_utc_time     TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE darts.user_account
IS 'migration columns all sourced directly from dm_user_s, but only for rows where r_is_group = 0';
COMMENT ON COLUMN darts.user_account.usr_id
IS 'primary key of user_account';
COMMENT ON COLUMN darts.user_account.r_dm_user_s_object_id
IS 'internal Documentum primary key from dm_user_s';

-- primary keys

CREATE UNIQUE INDEX annotation_pk ON darts.annotation(ann_id);
ALTER  TABLE darts.annotation              ADD PRIMARY KEY USING INDEX annotation_pk;

CREATE UNIQUE INDEX court_case_pk ON darts.court_case(cas_id);
ALTER  TABLE darts.court_case              ADD PRIMARY KEY USING INDEX court_case_pk;

CREATE UNIQUE INDEX courthouse_pk ON darts.courthouse(cth_id);
ALTER  TABLE darts.courthouse              ADD PRIMARY KEY USING INDEX courthouse_pk;

CREATE UNIQUE INDEX courtroom_pk ON darts.courtroom(ctr_id);
ALTER  TABLE darts.courtroom               ADD PRIMARY KEY USING INDEX courtroom_pk;

CREATE UNIQUE INDEX daily_list_pk ON darts.daily_list(dal_id);
ALTER  TABLE darts.daily_list              ADD PRIMARY KEY USING INDEX daily_list_pk;

CREATE UNIQUE INDEX event_pk ON darts.event(eve_id);
ALTER  TABLE darts.event                   ADD PRIMARY KEY USING INDEX event_pk;

CREATE UNIQUE INDEX event_type_pk ON darts.event_type(evt_id);
ALTER  TABLE darts.event_type              ADD PRIMARY KEY USING INDEX event_type_pk;

CREATE UNIQUE INDEX external_object_directory_pk ON darts.external_object_directory(eod_id);
ALTER  TABLE darts.external_object_directory   ADD PRIMARY KEY USING INDEX external_object_directory_pk;

CREATE UNIQUE INDEX hearing_pk ON darts.hearing(hea_id);
ALTER  TABLE darts.hearing                 ADD PRIMARY KEY USING INDEX hearing_pk;

CREATE UNIQUE INDEX hearing_event_ae_pk ON darts.hearing_event_ae(hev_id);
ALTER  TABLE darts.hearing_event_ae        ADD PRIMARY KEY USING INDEX hearing_event_ae_pk;

CREATE UNIQUE INDEX hearing_media_ae_pk ON darts.hearing_media_ae(hma_id);
ALTER  TABLE darts.hearing_media_ae        ADD PRIMARY KEY USING INDEX hearing_media_ae_pk;

CREATE UNIQUE INDEX media_pk ON darts.media(med_id);
ALTER  TABLE darts.media                   ADD PRIMARY KEY USING INDEX media_pk;

CREATE UNIQUE INDEX media_request_pk ON darts.media_request(mer_id);
ALTER  TABLE darts.media_request           ADD PRIMARY KEY USING INDEX media_request_pk;

CREATE UNIQUE INDEX notification_pk ON darts.notification(not_id);
ALTER  TABLE darts.notification            ADD PRIMARY KEY USING INDEX notification_pk;

CREATE UNIQUE INDEX object_directory_status_pk ON darts.object_directory_status(ods_id);
ALTER  TABLE darts.object_directory_status ADD PRIMARY KEY USING INDEX object_directory_status_pk;

CREATE UNIQUE INDEX report_pk ON darts.report(rep_id);
ALTER  TABLE darts.report                  ADD PRIMARY KEY USING INDEX report_pk;

CREATE UNIQUE INDEX reporting_restrictons_pk ON darts.reporting_restrictions(rer_id);
ALTER  TABLE darts.reporting_restrictions  ADD PRIMARY KEY USING INDEX reporting_restrictons_pk;

CREATE UNIQUE INDEX transcription_pk ON darts.transcription(tra_id);
ALTER  TABLE darts.transcription           ADD PRIMARY KEY USING INDEX transcription_pk;

CREATE UNIQUE INDEX transcription_comment_pk ON darts.transcription_comment(trc_id);
ALTER  TABLE darts.transcription_comment   ADD PRIMARY KEY USING INDEX transcription_comment_pk;

CREATE UNIQUE INDEX transcription_type_pk ON darts.transcription_type(trt_id);
ALTER  TABLE darts.transcription_type      ADD PRIMARY KEY USING INDEX transcription_type_pk;

CREATE UNIQUE INDEX transient_object_directory_pk ON darts.transient_object_directory(tod_id);
ALTER  TABLE darts.transient_object_directory  ADD PRIMARY KEY USING INDEX transient_object_directory_pk;

CREATE UNIQUE INDEX urgency_pk ON darts.urgency(urg_id);
ALTER  TABLE darts.urgency                 ADD PRIMARY KEY USING INDEX urgency_pk;

CREATE UNIQUE INDEX user_account_pk ON darts.user_account( usr_id);
ALTER  TABLE darts.user_account            ADD PRIMARY KEY USING INDEX user_account_pk;

-- defaults for postgres sequences, datatype->bigint, increment->1, nocycle is default, owned by none
CREATE SEQUENCE darts.ann_seq CACHE 20;
CREATE SEQUENCE darts.cas_seq CACHE 20;
CREATE SEQUENCE darts.cth_seq CACHE 20;
CREATE SEQUENCE darts.ctr_seq CACHE 20;
CREATE SEQUENCE darts.dal_seq CACHE 20;
CREATE SEQUENCE darts.eve_seq CACHE 20;
CREATE SEQUENCE darts.evt_seq CACHE 20;
CREATE SEQUENCE darts.eod_seq CACHE 20;
CREATE SEQUENCE darts.hea_seq CACHE 20;
CREATE SEQUENCE darts.hev_seq CACHE 20;
CREATE SEQUENCE darts.hma_seq CACHE 20;
CREATE SEQUENCE darts.med_seq CACHE 20;
CREATE SEQUENCE darts.mer_seq CACHE 20;
CREATE SEQUENCE darts.not_seq CACHE 20;
CREATE SEQUENCE darts.ods_seq CACHE 20;
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
ADD CONSTRAINT case_reporting_restriction_fk
FOREIGN KEY (rer_id) REFERENCES darts.reporting_restrictions(rer_id);

ALTER TABLE darts.hearing
ADD CONSTRAINT hearing_case_fk
FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.courtroom
ADD CONSTRAINT courtroom_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.daily_list
ADD CONSTRAINT daily_list_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.event
ADD CONSTRAINT event_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.event
ADD CONSTRAINT event_event_type_fk
FOREIGN KEY (evt_id) REFERENCES darts.event_type(evt_id);

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
FOREIGN KEY (modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.external_object_directory
ADD CONSTRAINT eod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES darts.object_directory_status(ods_id);

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
FOREIGN KEY (modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transient_object_directory
ADD CONSTRAINT tod_media_request_fk
FOREIGN KEY (mer_id) REFERENCES darts.media_request(mer_id);

ALTER TABLE darts.transient_object_directory
ADD CONSTRAINT tod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES darts.object_directory_status(ods_id);

-- additional unique multi-column indexes and constraints

--,UNIQUE (cth_id,courtroom_name)
CREATE UNIQUE INDEX ctr_chr_crn_unq ON darts.courtroom( cth_id, courtroom_name);
ALTER  TABLE darts.courtroom ADD UNIQUE USING INDEX ctr_chr_crn_unq;

--,UNIQUE(cas_id,ctr_id,c_hearing_date)
CREATE UNIQUE INDEX hea_cas_ctr_hd_unq ON darts.hearing( cas_id, ctr_id,c_hearing_date);
ALTER  TABLE darts.hearing ADD UNIQUE USING INDEX hea_cas_ctr_hd_unq;

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

