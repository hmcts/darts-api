-- List of Table Aliases
-- moj_annotation                ANN
-- moj_cached_media              CAM
-- moj_case                      CAS
-- moj_case_event_ae             CEA
-- moj_case_media_ae             CMA
-- moj_courthouse                CRT
-- moj_daily_list                DAL
-- moj_event                     EVE
-- moj_hearing                   HEA
-- moj_media                     MED
-- moj_report                    REP
-- moj_transcription             TRA
-- moj_transcription_comment     TRC
-- moj_transformation_log        TRL
-- moj_transformation_request    TRR


CREATE TABLE DARTS.moj_annotation
(moj_ann_id               INTEGER
,moj_cas_id               INTEGER
,r_annotation_object_id   CHARACTER VARYING(16)
,c_text                   CHARACTER VARYING(2000)
,c_time_stamp             DATE
,c_start                  DATE
,c_end                    DATE
,c_courthouse             CHARACTER VARYING(64)
,c_courtroom              CHARACTER VARYING(64)
,c_reporting_restrictions NUMERIC(6)
,r_case_object_id         CHARACTER VARYING(16)
,r_version_label          CHARACTER VARYING(32)
,i_superseded             BOOLEAN
,i_version_label          SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_annotation.moj_ann_id
IS 'primary key of moj_annotation';

COMMENT ON COLUMN DARTS.moj_annotation.moj_cas_id
IS 'foreign key from moj_case';

COMMENT ON COLUMN DARTS.moj_annotation.r_annotation_object_id
IS 'internal Documentum primary key from moj_annotation_s';

COMMENT ON COLUMN DARTS.moj_annotation.c_text
IS 'directly sourced from moj_annotation_s';

COMMENT ON COLUMN DARTS.moj_annotation.c_time_stamp
IS 'directly sourced from moj_annotation_s';

COMMENT ON COLUMN DARTS.moj_annotation.c_start
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_annotation.c_end
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_annotation.c_courthouse
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_annotation.c_courtroom
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_annotation.c_reporting_restrictions
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_annotation.r_case_object_id
IS 'internal Documentum id from moj_case_s acting as foreign key';

COMMENT ON COLUMN DARTS.moj_annotation.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_annotation';

CREATE TABLE DARTS.moj_cached_media
(moj_cam_id                INTEGER
,moj_cas_id                INTEGER
,r_cached_media_object_id  CHARACTER VARYING(16)
,c_last_accessed           DATE
,c_log_id                  CHARACTER VARYING(16)
,c_channel                 NUMERIC(10)
,c_total_channels          NUMERIC(10)
,c_reference_id            CHARACTER VARYING(32)
,c_start                   DATE
,c_end                     DATE
,c_courthouse              CHARACTER VARYING(64)
,c_courtroom               CHARACTER VARYING(64)
,c_reporting_restrictions  NUMERIC(6)
,r_case_object_id          CHARACTER VARYING(32)
,r_version_label           CHARACTER VARYING(32)
,i_superseded              BOOLEAN
,i_version_label           SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_cached_media.moj_cam_id
IS 'primary key of moj_cached_media';

COMMENT ON COLUMN DARTS.moj_cached_media.moj_cas_id
IS 'foreign key from moj_case';

COMMENT ON COLUMN DARTS.moj_cached_media.r_cached_media_object_id
IS 'internal Documentum primary key from moj_cached_media_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_last_accessed
IS 'directly sourced from moj_cached_media_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_log_id
IS 'directly sourced from moj_cached_media_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_channel
IS 'inherited from moj_media_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_total_channels
IS 'inherited from moj_media_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_reference_id
IS 'inherited from moj_media_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_start
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_end
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_courthouse
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_courtroom
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_cached_media.c_reporting_restrictions
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_cached_media.r_case_object_id
IS 'internal Documentum id from moj_case_s acting as foreign key';

COMMENT ON COLUMN DARTS.moj_cached_media.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_cached_media';

CREATE TABLE DARTS.moj_case
(moj_cas_id                INTEGER
,moj_crt_id                INTEGER
,r_case_object_id          CHARACTER VARYING(16)
,c_type                    CHARACTER VARYING(32)
,c_case_id                 CHARACTER VARYING(32)
,c_courthouse              CHARACTER VARYING(64)
,c_courtroom               CHARACTER VARYING(64)
,c_scheduled_start         DATE
,c_upload_priority         NUMERIC(10)
,c_reporting_restrictions  CHARACTER VARYING(128)
,c_closed                  NUMERIC(6)
,c_interpreter_used        NUMERIC(1)
,c_case_closed_date        DATE
,r_courthouse_object_id    CHARACTER VARYING(16)
,r_version_label           CHARACTER VARYING(32)
,i_superseded              BOOLEAN
,i_version_label           SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_case.moj_cas_id
IS 'primary key of moj_case';

COMMENT ON COLUMN DARTS.moj_case.moj_crt_id
IS 'foreign key from moj_courthouse';

COMMENT ON COLUMN DARTS.moj_case.r_case_object_id
IS 'internal Documentum primary key from moj_cached_media_s';

COMMENT ON COLUMN DARTS.moj_case.c_type
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_case_id
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_courthouse
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_courtroom
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_scheduled_start
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_upload_priority
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_reporting_restrictions
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_closed
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_interpreter_used
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.c_case_closed_date
IS 'directly sourced from moj_case_s';

COMMENT ON COLUMN DARTS.moj_case.r_courthouse_object_id
IS 'internal Documentum id from moj_courthouse_s acting as foreign key';

COMMENT ON COLUMN DARTS.moj_case.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_case';

CREATE TABLE DARTS.moj_case_event_ae
(moj_cea_id         INTEGER
,moj_cas_id         INTEGER
,moj_eve_id         INTEGER);

COMMENT ON COLUMN DARTS.moj_case_event_ae.moj_cea_id
IS 'primary key of case_event_ae';

COMMENT ON COLUMN DARTS.moj_case_event_ae.moj_cas_id
IS 'foreign key from moj_case, part of composite natural key';

COMMENT ON COLUMN DARTS.moj_case_event_ae.moj_eve_id
IS 'foreign key from moj_event, part of composite natural key';

CREATE TABLE DARTS.moj_case_media_ae
(moj_cma_id         INTEGER 
,moj_cas_id         INTEGER
,moj_med_id         INTEGER);

COMMENT ON COLUMN DARTS.moj_case_media_ae.moj_cas_id
IS 'primary key of moj_case_media_ae';

COMMENT ON COLUMN DARTS.moj_case_media_ae.moj_cas_id
IS 'foreign key from moj_case, part of composite natural key';

COMMENT ON COLUMN DARTS.moj_case_media_ae.moj_med_id
IS 'foreign key from moj_media, part of composite natural key';

CREATE TABLE DARTS.moj_courthouse
(moj_crt_id                INTEGER
,r_courthouse_object_id    CHARACTER VARYING(16)
,c_code                    CHARACTER VARYING(32)
,c_id                      CHARACTER VARYING(255)
,c_alias_set_id            CHARACTER VARYING(16)
,r_version_label           CHARACTER VARYING(32)
,i_superseded              BOOLEAN
,i_version_label           SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_courthouse.moj_crt_id
IS 'primary key of moj_courthouse';

COMMENT ON COLUMN DARTS.moj_courthouse.r_courthouse_object_id
IS 'internal Documentum primary key from moj_courthouse_s';

COMMENT ON COLUMN DARTS.moj_courthouse.c_code
IS 'directly sourced from moj_courthouse_s';

COMMENT ON COLUMN DARTS.moj_courthouse.c_id
IS 'directly sourced from moj_courthouse_s';

COMMENT ON COLUMN DARTS.moj_courthouse.c_alias_set_id
IS 'directly sourced from moj_courthouse_s';

COMMENT ON COLUMN DARTS.moj_courthouse.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_courthouse';

CREATE TABLE DARTS.moj_daily_list
(moj_dal_id                 INTEGER
,moj_crt_id                 INTEGER
,r_daily_list_object_id     CHARACTER VARYING(16)
,c_unique_id                CHARACTER VARYING(200)
,c_crown_court_name         CHARACTER VARYING(200)
,c_job_status               CHARACTER VARYING(20)
,c_timestamp                DATE 
,c_crown_court_code         CHARACTER VARYING(100)
,c_daily_list_id            NUMERIC(10)
,c_start_date               DATE   
,c_end_date                 DATE 
,c_daily_list_id_s          CHARACTER VARYING(100)
,c_daily_list_source        CHARACTER VARYING(3)
,r_courthouse_object_id     CHARACTER VARYING(16)
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version_label            SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_daily_list.moj_dal_id
IS 'primary key of moj_daily_list';

COMMENT ON COLUMN DARTS.moj_daily_list.moj_crt_id
IS 'foreign key from moj_courthouse';

COMMENT ON COLUMN DARTS.moj_daily_list.r_daily_list_object_id
IS 'internal Documentum primary key from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_unique_id
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_crown_court_name
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_job_status
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_timestamp
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_crown_court_code
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_daily_list_id
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_start_date
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_end_date
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_daily_list_id_s
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.c_daily_list_source
IS 'directly sourced from moj_daily_list_s';

COMMENT ON COLUMN DARTS.moj_daily_list.r_courthouse_object_id
IS 'must be inferred from c_crown_court_code and/or c_crown_court_name';

COMMENT ON COLUMN DARTS.moj_daily_list.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_daily_list';

CREATE TABLE DARTS.moj_event
(moj_eve_id                 INTEGER
,r_event_object_id          CHARACTER VARYING(16)
,c_event_id                 NUMERIC(10)
,c_text	                    CHARACTER VARYING(2000)
,c_time_stamp               DATE  
,c_start                    DATE 
,c_end                      DATE 
,c_courthouse               CHARACTER VARYING(64)
,c_courtroom                CHARACTER VARYING(64)
,c_reporting_restrictions   NUMERIC(6)
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version_label            SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_event.moj_eve_id
IS 'primary key of moj_event';

COMMENT ON COLUMN DARTS.moj_event.r_event_object_id
IS 'internal Documentum primary key from moj_event_s';

COMMENT ON COLUMN DARTS.moj_event.c_event_id
IS 'directly sourced from moj_event_s';

COMMENT ON COLUMN DARTS.moj_event.c_text
IS 'inherited from moj_annotation_s';

COMMENT ON COLUMN DARTS.moj_event.c_time_stamp
IS 'inherited from moj_annotation_s';

COMMENT ON COLUMN DARTS.moj_event.c_start
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_event.c_end
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_event.c_courthouse
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_event.c_courtroom
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_event.c_reporting_restrictions
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_event.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_event';

CREATE TABLE DARTS.moj_hearing
(moj_hea_id                 INTEGER
,moj_cas_id                 INTEGER          
,c_judge                    CHARACTER VARYING(2000)
,c_defendant                CHARACTER VARYING(2000)
,c_prosecutor               CHARACTER VARYING(2000)
,c_defence                  CHARACTER VARYING(2000)
,c_hearing_date             DATE
,c_judge_hearing_date       CHARACTER VARYING(2000)
,r_case_object_id           CHARACTER VARYING(16)
--,r_version_label            CHARACTER VARYING(32) 
,i_superseded               BOOLEAN
,i_version_label            SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_hearing.moj_hea_id
IS 'primary key of moj_hearing';

COMMENT ON COLUMN DARTS.moj_hearing.moj_cas_id
IS 'foreign key of moj_case';

COMMENT ON COLUMN DARTS.moj_hearing.c_judge
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN DARTS.moj_hearing.c_defendant
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN DARTS.moj_hearing.c_prosecutor
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN DARTS.moj_hearing.c_defence
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN DARTS.moj_hearing.c_hearing_date
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN DARTS.moj_hearing.c_judge_hearing_date
IS 'directly sourced from moj_case_r';

COMMENT ON COLUMN DARTS.moj_hearing.r_case_object_id
IS 'internal Documentum id from moj_case_s acting as foreign key';

CREATE TABLE DARTS.moj_media
(moj_med_id                 INTEGER
,r_media_object_id          CHARACTER VARYING(16)
,c_channel                  NUMERIC(10)
,c_total_channels           NUMERIC(10)
,c_reference_id             CHARACTER VARYING(32)
,c_start                    DATE
,c_end                      DATE
,c_courthouse               CHARACTER VARYING(64)
,c_courtroom                CHARACTER VARYING(64)
,c_reporting_restrictions   NUMERIC(6)
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version_label            SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_media.moj_med_id
IS 'primary key of moj_media';

COMMENT ON COLUMN DARTS.moj_media.r_media_object_id
IS 'internal Documentum primary key from moj_media_s';

COMMENT ON COLUMN DARTS.moj_media.c_channel
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN DARTS.moj_media.c_total_channels
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN DARTS.moj_media.c_reference_id
IS 'directly sourced from moj_media_s';

COMMENT ON COLUMN DARTS.moj_media.c_start
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_media.c_end
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_media.c_courthouse
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_media.c_courtroom
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_media.c_reporting_restrictions
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_media.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_media';

CREATE TABLE DARTS.moj_report               
(moj_rep_id                 INTEGER
,r_report_object_id         CHARACTER VARYING(16)
,c_name                     CHARACTER VARYING(32) 
,c_subject                  CHARACTER VARYING(256)
,c_text                     CHARACTER VARYING(1024)
,c_query                    CHARACTER VARYING(2048)
,c_recipients               CHARACTER VARYING(1024)
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version_label            SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_report.moj_rep_id
IS 'primary key of moj_report';

COMMENT ON COLUMN DARTS.moj_report.r_report_object_id
IS 'internal Documentum primary key from moj_report_s';

COMMENT ON COLUMN DARTS.moj_report.c_name
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN DARTS.moj_report.c_subject
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN DARTS.moj_report.c_text
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN DARTS.moj_report.c_query
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN DARTS.moj_report.c_recipients
IS 'directly sourced from moj_report_s';

COMMENT ON COLUMN DARTS.moj_report.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_report';

CREATE TABLE DARTS.moj_transcription
(moj_tra_id                 INTEGER
,moj_cas_id                 INTEGER
,r_transcription_object_id  CHARACTER VARYING(16)
,c_company                  CHARACTER VARYING(64)
,c_type                     NUMERIC(10)
,c_notification_type        CHARACTER VARYING(64)
,c_urgent                   NUMERIC(6)
,c_requestor                CHARACTER VARYING(32)
,c_current_state            CHARACTER VARYING(32)
,c_urgency                  CHARACTER VARYING(32)
,c_hearing_date             DATE
,c_start                    DATE
,c_end                      DATE
,c_courthouse               CHARACTER VARYING(64)
,c_courtroom                CHARACTER VARYING(64)
,c_reporting_restrictions   NUMERIC(6)
,r_case_object_id           CHARACTER VARYING(16)
,r_version_label            CHARACTER VARYING(32)
,i_superseded               BOOLEAN
,i_version_label            SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_transcription.moj_tra_id
IS 'primary key of moj_transcription';
    
COMMENT ON COLUMN DARTS.moj_transcription.moj_cas_id
IS 'foreign key from moj_case';

COMMENT ON COLUMN DARTS.moj_transcription.r_transcription_object_id
IS 'internal Documentum primary key from moj_transcription_s';
    
COMMENT ON COLUMN DARTS.moj_transcription.c_company
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_type
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_notification_type
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_urgent
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_requestor
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_current_state
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_urgency
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_hearing_date
IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_start
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_end
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_courthouse
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_courtroom
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transcription.c_reporting_restrictions
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transcription.r_case_object_id
IS 'internal Documentum id from moj_case_s acting as foreign key';

COMMENT ON COLUMN DARTS.moj_transcription.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_transcription';

CREATE TABLE DARTS.moj_transcription_comment
(moj_trc_id                        INTEGER
,moj_tra_id                        INTEGER
,r_transcription_comment_object_id CHARACTER VARYING(16)
,c_comment                         CHARACTER VARYING(1024)
,r_transcription_object_id         CHARACTER VARYING(16)
--,r_version_label                   CHARACTER VARYING(32)
,i_superseded                      BOOLEAN
,i_version_label                   SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_transcription_comment.moj_trc_id
IS 'primary key of moj_transcription_comment'; 

COMMENT ON COLUMN DARTS.moj_transcription_comment.moj_tra_id
IS 'foreign key from moj_case'; 

COMMENT ON COLUMN DARTS.moj_transcription_comment.r_transcription_comment_object_id
IS 'internal Documentum primary key from moj_transcription_s'; 

COMMENT ON COLUMN DARTS.moj_transcription_comment.c_comment
IS 'directly sourced from moj_transcription_r';

COMMENT ON COLUMN DARTS.moj_transcription_comment.r_transcription_object_id
IS 'internal Documentum id from moj_transcription_s acting as foreign key';

CREATE TABLE DARTS.moj_transformation_log
(moj_trl_id                        INTEGER
,moj_cas_id                        INTEGER
,r_transformation_log_object_id    CHARACTER VARYING(16)
,c_case_id                         CHARACTER VARYING(32)   
,c_courthouse                      CHARACTER VARYING(64)
,c_requested_date                  DATE
,c_received_date                   DATE
,r_case_object_id                  CHARACTER VARYING(16)
,r_version_label                   CHARACTER VARYING(32)
,i_superseded                      BOOLEAN
,i_version_label                   SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_transformation_log.moj_trl_id
IS 'primary key of moj_transformation_log';

COMMENT ON COLUMN DARTS.moj_transformation_log.moj_cas_id
IS 'foreign key from moj_case';

COMMENT ON COLUMN DARTS.moj_transformation_log.r_transformation_log_object_id
IS 'internal Documentum primary key from moj_transformation_log_s';

COMMENT ON COLUMN DARTS.moj_transformation_log.c_case_id
IS 'directly sourced from moj_transformation_log_s';

COMMENT ON COLUMN DARTS.moj_transformation_log.c_courthouse
IS 'directly sourced from moj_transformation_log_s';

COMMENT ON COLUMN DARTS.moj_transformation_log.c_requested_date
IS 'directly sourced from moj_transformation_log_s';

COMMENT ON COLUMN DARTS.moj_transformation_log.c_received_date
IS 'directly sourced from moj_transformation_log_s';

COMMENT ON COLUMN DARTS.moj_transformation_log.r_case_object_id
IS 'directly sourced from moj_transformation_log_s';

COMMENT ON COLUMN DARTS.moj_transformation_log.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_transformation_log';

CREATE TABLE DARTS.moj_transformation_request
(moj_trr_id                          INTEGER
,moj_cas_id                          INTEGER
,r_transformation_request_object_id  CHARACTER VARYING(16) 
,c_type                              CHARACTER VARYING(12)
,c_output_format                     CHARACTER VARYING(12)
,c_audio_folder_id                   CHARACTER VARYING(16)
,c_output_file                       CHARACTER VARYING(100)
,c_requestor                         CHARACTER VARYING(32)
,c_court_log_id                      CHARACTER VARYING(16)
,c_priority                          NUMERIC(10)
,c_channel                           NUMERIC(10)
,c_total_channels                    NUMERIC(10)
,c_reference_id                      CHARACTER VARYING(32)
,c_start                             DATE
,c_end                               DATE
,c_courthouse                        CHARACTER VARYING(64)
,c_courtroom                         CHARACTER VARYING(64)
,c_reporting_restrictions            NUMERIC(6)
,r_case_object_id                    CHARACTER VARYING(16)
,r_version_label                     CHARACTER VARYING(32)
,i_superseded                        BOOLEAN
,i_version_label                     SMALLSERIAL);

COMMENT ON COLUMN DARTS.moj_transformation_request.moj_trr_id
IS 'primary key from moj_transformation_request';

COMMENT ON COLUMN DARTS.moj_transformation_request.moj_cas_id
IS 'foreign key from moj_case';

COMMENT ON COLUMN DARTS.moj_transformation_request.r_transformation_request_object_id
IS 'internal Documentum primary key from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_type
IS 'directly sourced from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_output_format
IS 'directly sourced from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_audio_folder_id
IS 'directly sourced from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_output_file
IS 'directly sourced from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_requestor
IS 'directly sourced from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_court_log_id
IS 'directly sourced from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_priority
IS 'directly sourced from moj_transformation_request_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_channel
IS 'inherited from moj_media_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_total_channels
IS 'inherited from moj_media_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_reference_id
IS 'inherited from moj_media_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_start
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_end
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_courthouse
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_courtroom
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.c_reporting_restrictions
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.r_case_object_id
IS 'inherited from moj_case_document_s';

COMMENT ON COLUMN DARTS.moj_transformation_request.r_version_label
IS 'inherited from dm_sysobject_r, for r_object_type of moj_transformaton_request';


ALTER TABLE DARTS.moj_annotation                ADD PRIMARY KEY (moj_ann_id);
ALTER TABLE DARTS.moj_cached_media              ADD PRIMARY KEY (moj_cam_id);
ALTER TABLE DARTS.moj_case                      ADD PRIMARY KEY (moj_cas_id);
ALTER TABLE DARTS.moj_case_event_ae             ADD PRIMARY KEY (moj_cas_id,moj_eve_id);
ALTER TABLE DARTS.moj_case_media_ae             ADD PRIMARY KEY (moj_cas_id,moj_med_id);
ALTER TABLE DARTS.moj_courthouse                ADD PRIMARY KEY (moj_crt_id);
ALTER TABLE DARTS.moj_daily_list                ADD PRIMARY KEY (moj_dal_id);
ALTER TABLE DARTS.moj_event                     ADD PRIMARY KEY (moj_eve_id);
ALTER TABLE DARTS.moj_hearing                   ADD PRIMARY KEY (moj_hea_id);
ALTER TABLE DARTS.moj_media                     ADD PRIMARY KEY (moj_med_id);
ALTER TABLE DARTS.moj_report                    ADD PRIMARY KEY (moj_rep_id);
ALTER TABLE DARTS.moj_transcription             ADD PRIMARY KEY (moj_tra_id);
ALTER TABLE DARTS.moj_transcription_comment     ADD PRIMARY KEY (moj_trc_id);
ALTER TABLE DARTS.moj_transformation_log        ADD PRIMARY KEY (moj_trl_id);
ALTER TABLE DARTS.moj_transformation_request    ADD PRIMARY KEY (moj_trr_id);

ALTER TABLE DARTS.moj_annotation                ADD CONSTRAINT moj_annotation_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);

ALTER TABLE DARTS.moj_cached_media              ADD CONSTRAINT moj_cached_media_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);

ALTER TABLE DARTS.moj_case                      ADD CONSTRAINT moj_case_courthouse_fk
FOREIGN KEY (moj_crt_id) REFERENCES DARTS.moj_courthouse(moj_crt_id);

ALTER TABLE DARTS.moj_case_event_ae             ADD CONSTRAINT moj_case_event_ae_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);

ALTER TABLE DARTS.moj_case_event_ae             ADD CONSTRAINT moj_case_event_ae_event_fk
FOREIGN KEY (moj_eve_id) REFERENCES DARTS.moj_event(moj_eve_id);

ALTER TABLE DARTS.moj_case_media_ae             ADD CONSTRAINT moj_case_media_ae_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);

ALTER TABLE DARTS.moj_case_media_ae             ADD CONSTRAINT moj_case_media_ae_media_fk
FOREIGN KEY (moj_med_id) REFERENCES DARTS.moj_media(moj_med_id);

ALTER TABLE DARTS.moj_daily_list                ADD CONSTRAINT moj_daily_list_courthouse_fk
FOREIGN KEY (moj_crt_id) REFERENCES DARTS.moj_courthouse(moj_crt_id);

ALTER TABLE DARTS.moj_hearing                   ADD CONSTRAINT moj_hearing_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);

ALTER TABLE DARTS.moj_transcription             ADD CONSTRAINT moj_transcription_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);

ALTER TABLE DARTS.moj_transcription_comment     ADD CONSTRAINT moj_transcription_comment_transcription_fk
FOREIGN KEY (moj_tra_id) REFERENCES DARTS.moj_transcription(moj_tra_id);

ALTER TABLE DARTS.moj_transformation_log        ADD CONSTRAINT moj_transformation_log_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);

ALTER TABLE DARTS.moj_transformation_request    ADD CONSTRAINT moj_transformation_request_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES DARTS.moj_case(moj_cas_id);
