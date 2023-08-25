ALTER TABLE annotation DROP COLUMN superseded;
ALTER TABLE annotation DROP COLUMN version;
ALTER TABLE annotation ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE annotation ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE annotation ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE annotation ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE audit_activities RENAME column id to aua_id;
ALTER TABLE audit_activities RENAME column name to activity_name;
ALTER TABLE audit_activities RENAME column description to activity_description;
ALTER TABLE audit_activities ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE audit_activities ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE audit_activities ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE audit_activities ADD COLUMN last_modified_by            INTEGER                       ;



ALTER TABLE audit RENAME column id to aud_id;
ALTER TABLE audit RENAME column case_id to cas_id;
ALTER TABLE audit RENAME column audit_activity_id to aua_id;
ALTER TABLE audit RENAME column user_id to usr_id;
ALTER TABLE audit ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE audit ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE audit ADD COLUMN last_modified_by            INTEGER                       ;
COMMENT ON COLUMN audit.aud_id
IS 'primary key of audit';
COMMENT ON COLUMN audit.cas_id
IS 'foreign key from case';
COMMENT ON COLUMN audit.aua_id
IS 'foreign key from audit_activity';
COMMENT ON COLUMN audit.usr_id
IS 'foreign key from user_account';

alter table audit_activities rename to audit_activity;


ALTER TABLE automated_task ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE automated_task ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE automated_task ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE automated_task ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE case_retention ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE case_retention ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE case_retention ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE case_retention ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE case_retention_event ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE case_retention_event ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE case_retention_event ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE case_retention_event ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE court_case DROP COLUMN superseded;
ALTER TABLE court_case DROP COLUMN version;
ALTER TABLE court_case ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE court_case ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE court_case ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE court_case ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE courthouse ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE courthouse ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE courtroom ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE courtroom ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE courtroom ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE courtroom ADD COLUMN last_modified_by            INTEGER                       ;


ALTER TABLE daily_list DROP COLUMN superseded;
ALTER TABLE daily_list DROP COLUMN version;
ALTER TABLE daily_list ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE daily_list ADD COLUMN last_modified_by            INTEGER                       ;
ALTER TABLE daily_list RENAME column daily_list_content to daily_list_content_json;
ALTER TABLE daily_list ADD COLUMN daily_list_content_xml      CHARACTER VARYING;

ALTER TABLE defence ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE defence ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE defence ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE defence ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE defendant ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE defendant ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE defendant ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE defendant ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE node_register ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE node_register ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE node_register ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE node_register ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE event DROP COLUMN superseded;
ALTER TABLE event DROP COLUMN version;
ALTER TABLE event ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE event ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE event ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE event ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE event_handler ADD COLUMN created_by                  INTEGER                       ;

ALTER TABLE external_object_directory ADD COLUMN created_by                  INTEGER                       ;

ALTER TABLE external_location_type ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE external_location_type ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE external_location_type ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE external_location_type ADD COLUMN last_modified_by            INTEGER                       ;

CREATE TABLE external_service_auth_token
(esa_id                      INTEGER                       NOT NULL
,external_service_userid     CHARACTER VARYING             NOT NULL
,token_type                  INTEGER                       NOT NULL
,token                       CHARACTER VARYING             NOT NULL
,expiry_ts                   TIMESTAMP WITH TIME ZONE
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER
);

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


ALTER TABLE hearing ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE hearing ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE hearing ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE hearing ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE judge ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE judge ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE judge ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE judge ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE media DROP COLUMN superseded;
ALTER TABLE media DROP COLUMN version;
ALTER TABLE media ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE media ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE media ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE media ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE media_request ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE media_request ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE notification ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE notification ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE object_directory_status ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE object_directory_status ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE object_directory_status ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE object_directory_status ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE prosecutor ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE prosecutor ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE prosecutor ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE prosecutor ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE region ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE region ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE region ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE region ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE report ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE report ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE report ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE report ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE retention_policy ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE retention_policy ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE retention_policy ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE retention_policy ADD COLUMN last_modified_by            INTEGER                       ;

CREATE TABLE transcription_urgency
(tru_id                      INTEGER                       NOT NULL
,description                 CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER
);

COMMENT ON TABLE transcription_urgency
IS 'will be migrated from tbl_moj_urgency';

COMMENT ON COLUMN transcription_urgency.tru_id
IS 'inherited from tbl_moj_urgency.urgency_id';

COMMENT ON COLUMN transcription_urgency.description
IS 'inherited from tbl_moj_urgency.description';


ALTER TABLE transcription DROP COLUMN superseded;
ALTER TABLE transcription DROP COLUMN version;
ALTER TABLE transcription ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE transcription ADD COLUMN tru_id                  INTEGER                       ;
ALTER TABLE transcription DROP COLUMN urg_id;


ALTER TABLE transcription_comment DROP COLUMN superseded;
ALTER TABLE transcription_comment DROP COLUMN version;
ALTER TABLE transcription_comment ADD COLUMN created_by                  INTEGER                       ;

ALTER TABLE transcription_type ADD COLUMN created_ts                  TIMESTAMP WITH TIME ZONE      ;
ALTER TABLE transcription_type ADD COLUMN created_by                  INTEGER                       ;
ALTER TABLE transcription_type ADD COLUMN last_modified_ts            TIMESTAMP WITH TIME ZONE     ;
ALTER TABLE transcription_type ADD COLUMN last_modified_by            INTEGER                       ;

ALTER TABLE transient_object_directory ADD COLUMN created_by                  INTEGER                       ;

DROP TABLE urgency cascade;


ALTER TABLE user_account ADD COLUMN created_by                  INTEGER                       ;



CREATE SEQUENCE aud_seq CACHE 20;
CREATE SEQUENCE aua_seq CACHE 20 RESTART WITH 8;
CREATE SEQUENCE esa_seq CACHE 20;
CREATE SEQUENCE tru_seq CACHE 20;

DROP SEQUENCE urg_seq;

