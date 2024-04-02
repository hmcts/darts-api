ALTER TABLE annotation ALTER COLUMN is_deleted SET DEFAULT false;

ALTER TABLE annotation_document ADD COLUMN ohr_id INTEGER;
ALTER TABLE annotation_document ALTER COLUMN checksum DROP NOT NULL;
ALTER TABLE annotation_document ALTER COLUMN is_hidden SET DEFAULT false;
ALTER TABLE annotation_document ADD COLUMN marked_for_manual_deletion BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE annotation_document ADD COLUMN retain_until_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE annotation_document ADD COLUMN last_modified_ts TIMESTAMP WITH TIME ZONE;
update annotation_document set last_modified_ts = uploaded_ts;
ALTER TABLE annotation_document ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE annotation_document ADD COLUMN last_modified_by INTEGER;

ALTER TABLE audit ADD COLUMN enhanced_auditing BOOLEAN NOT NULL DEFAULT false;

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
 );


ALTER TABLE case_document ADD COLUMN ohr_id INTEGER;
ALTER TABLE case_document ALTER COLUMN checksum DROP NOT NULL;
ALTER TABLE case_document ALTER COLUMN is_hidden SET DEFAULT false;
ALTER TABLE case_document ADD COLUMN marked_for_manual_deletion BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE case_document ADD COLUMN retain_until_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_document ADD COLUMN last_modified_ts TIMESTAMP WITH TIME ZONE;
update case_document set last_modified_ts = uploaded_ts;
ALTER TABLE case_document ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE case_document ADD COLUMN last_modified_by INTEGER;

ALTER TABLE court_case ADD COLUMN is_retention_updated BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE court_case ADD COLUMN retention_retries INTEGER;
ALTER TABLE court_case ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE daily_list ADD COLUMN courthouse_code INTEGER;

ALTER TABLE external_object_directory ADD COLUMN osr_uuid CHARACTER VARYING;
ALTER TABLE external_object_directory ALTER COLUMN external_location DROP NOT NULL;
ALTER TABLE external_object_directory ADD COLUMN update_retention BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE media ADD COLUMN ohr_id INTEGER;
ALTER TABLE media ALTER COLUMN is_hidden SET DEFAULT false;
ALTER TABLE media ADD COLUMN marked_for_manual_deletion BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE media ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE media ADD COLUMN retain_until_ts TIMESTAMP WITH TIME ZONE;

CREATE TABLE object_hidden_reason
(ohr_id                      INTEGER                       NOT NULL
,ohr_reason                  CHARACTER VARYING             NOT NULL
,display_name                CHARACTER VARYING             NOT NULL
,display_state               BOOLEAN                       NOT NULL DEFAULT true
,display_order               INTEGER                       NOT NULL
,marked_for_deletion         BOOLEAN                       NOT NULL DEFAULT false
);

COMMENT ON TABLE object_hidden_reason
IS 'used to record acceptable reasons for data to be hidden in tables ADO,CAD, MED, TRD';

ALTER TABLE transcription ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE transcription_comment ALTER COLUMN tra_id DROP NOT NULL;

ALTER TABLE transcription_document ADD COLUMN ohr_id INTEGER;
ALTER TABLE transcription_document ALTER COLUMN checksum DROP NOT NULL;
ALTER TABLE transcription_document ALTER COLUMN is_hidden SET DEFAULT false;
ALTER TABLE transcription_document ADD COLUMN marked_for_manual_deletion BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE transcription_document ADD COLUMN retain_until_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE transcription_document ADD COLUMN last_modified_ts TIMESTAMP WITH TIME ZONE;
update transcription_document set last_modified_ts = uploaded_ts;
ALTER TABLE transcription_document ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE transcription_document ADD COLUMN last_modified_by INTEGER;

ALTER TABLE user_account ALTER COLUMN user_name DROP NOT NULL;
ALTER TABLE user_account ALTER COLUMN user_full_name DROP NOT NULL;
