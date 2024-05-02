CREATE TABLE event_linked_case
(elc_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       NOT NULL     -- unenforced FK to event
,cas_id                      INTEGER
,courthouse_name             CHARACTER VARYING(64)
,case_number                 CHARACTER VARYING(32)
);


CREATE TABLE media_linked_case
(mlc_id                      INTEGER                       NOT NULL
,med_id                      INTEGER                       NOT NULL     -- unenforced FK to media
,cas_id                      INTEGER
,courthouse_name             CHARACTER VARYING(64)
,case_number                 CHARACTER VARYING(32)
);

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
);

CREATE TABLE object_retrieval_queue
(orq_id                      INTEGER                       NOT NULL
,med_id                      INTEGER
,trd_id                      INTEGER
,parent_object_id            CHARACTER VARYING
,content_object_id           CHARACTER VARYING
,clip_id                     CHARACTER VARYING
,acknowledged_ts             TIMESTAMP WITH TIME ZONE
,migrated_ts                 TIMESTAMP WITH TIME ZONE
,status                      CHARACTER VARYING
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
);

CREATE SEQUENCE elc_seq CACHE 20;
CREATE SEQUENCE mlc_seq CACHE 20;
CREATE SEQUENCE oaa_seq CACHE 20;
CREATE SEQUENCE orq_seq CACHE 20;

ALTER TABLE annotation_document DROP CONSTRAINT annotation_document_object_hidden_reason_fk;
ALTER TABLE annotation_document DROP CONSTRAINT annotation_document_hidden_by_fk;
ALTER TABLE annotation_document DROP CONSTRAINT annotation_document_marked_for_manual_del_by_fk;

ALTER TABLE annotation_document DROP COLUMN ohr_id;
ALTER TABLE annotation_document DROP COLUMN hidden_by;
ALTER TABLE annotation_document DROP COLUMN hidden_ts;
ALTER TABLE annotation_document DROP COLUMN marked_for_manual_deletion;
ALTER TABLE annotation_document DROP COLUMN marked_for_manual_del_by;
ALTER TABLE annotation_document DROP COLUMN marked_for_manual_del_ts;

ALTER TABLE audit DROP COLUMN enhanced_auditing;

ALTER TABLE automated_task ADD COLUMN batch_size INTEGER;

ALTER TABLE case_document DROP CONSTRAINT case_document_object_hidden_reason_fk;
ALTER TABLE case_document DROP CONSTRAINT case_document_hidden_by_fk;
ALTER TABLE case_document DROP CONSTRAINT case_document_marked_for_manual_del_by_fk;

ALTER TABLE case_document DROP COLUMN ohr_id;
ALTER TABLE case_document DROP COLUMN hidden_by;
ALTER TABLE case_document DROP COLUMN hidden_ts;
ALTER TABLE case_document DROP COLUMN marked_for_manual_deletion;
ALTER TABLE case_document DROP COLUMN marked_for_manual_del_by;
ALTER TABLE case_document DROP COLUMN marked_for_manual_del_ts;

ALTER TABLE case_overflow ADD COLUMN checked_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_overflow ADD COLUMN corrected_ts                TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_overflow ADD COLUMN confidence_level            INTEGER;
ALTER TABLE case_overflow ADD COLUMN confidence_reason           CHARACTER VARYING;
ALTER TABLE case_overflow ADD COLUMN c_closed_pre_live           INTEGER;
ALTER TABLE case_overflow ADD COLUMN c_case_closed_date_pre_live TIMESTAMP WITH TIME ZONE;

ALTER TABLE court_case DROP COLUMN version_label;
ALTER TABLE court_case ADD COLUMN is_data_anonymised          BOOLEAN                       NOT NULL DEFAULT FALSE;
ALTER TABLE court_case ADD COLUMN data_anonymised_by          INTEGER;
ALTER TABLE court_case ADD COLUMN data_anonymised_ts          TIMESTAMP WITH TIME ZONE;

ALTER TABLE daily_list DROP COLUMN version_label;

ALTER TABLE event DROP COLUMN event_name;

ALTER TABLE media DROP CONSTRAINT media_object_hidden_reason_fk;
ALTER TABLE media DROP CONSTRAINT media_hidden_by_fk;
ALTER TABLE media DROP CONSTRAINT media_marked_for_manual_del_by_fk;

ALTER TABLE media DROP COLUMN ohr_id;
ALTER TABLE media DROP COLUMN hidden_by;
ALTER TABLE media DROP COLUMN hidden_ts;
ALTER TABLE media DROP COLUMN marked_for_manual_deletion;
ALTER TABLE media DROP COLUMN marked_for_manual_del_by;
ALTER TABLE media DROP COLUMN marked_for_manual_del_ts;

ALTER TABLE report DROP COLUMN version_label;

ALTER TABLE transcription_document DROP CONSTRAINT transcription_document_object_hidden_reason_fk;
ALTER TABLE transcription_document DROP CONSTRAINT transcription_document_hidden_by_fk;
ALTER TABLE transcription_document DROP CONSTRAINT transcription_document_marked_for_manual_del_by_fk;

ALTER TABLE transcription_document DROP COLUMN ohr_id;
ALTER TABLE transcription_document DROP COLUMN hidden_by;
ALTER TABLE transcription_document DROP COLUMN hidden_ts;
ALTER TABLE transcription_document DROP COLUMN marked_for_manual_deletion;
ALTER TABLE transcription_document DROP COLUMN marked_for_manual_del_by;
ALTER TABLE transcription_document DROP COLUMN marked_for_manual_del_ts;

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