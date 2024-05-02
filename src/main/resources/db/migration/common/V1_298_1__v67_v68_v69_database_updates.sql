CREATE TABLE event_linked_case
(elc_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       NOT NULL     -- unenforced FK to event
,cas_id                      INTEGER
,courthouse_name             CHARACTER VARYING(64)
,case_number                 CHARACTER VARYING(32)
);


CREATE TABLE media_linked_case
(mll_id                      INTEGER                       NOT NULL
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

CREATE UNIQUE INDEX event_linked_legacy_case_pk ON event_linked_legacy_case(ell_id);
ALTER TABLE event_linked_legacy_case  ADD PRIMARY KEY USING INDEX event_linked_legacy_case_pk;

CREATE UNIQUE INDEX media_linked_legacy_case_pk ON media_linked_legacy_case(mll_id);
ALTER TABLE media_linked_legacy_case  ADD PRIMARY KEY USING INDEX media_linked_legacy_case_pk;


CREATE SEQUENCE elc_seq CACHE 20;
CREATE SEQUENCE mlc_seq CACHE 20;
CREATE SEQUENCE oaa_seq CACHE 20;
CREATE SEQUENCE orq_seq CACHE 20;

ALTER TABLE annotation_document DROP COLUMN ohr_id;
ALTER TABLE annotation_document DROP COLUMN hidden_by;
ALTER TABLE annotation_document DROP COLUMN hidden_ts;
ALTER TABLE annotation_document DROP COLUMN marked_for_manual_deletion;
ALTER TABLE annotation_document DROP COLUMN marked_for_manual_del_by;
ALTER TABLE annotation_document DROP COLUMN marked_for_manual_del_ts;
ALTER TABLE annotation_document DROP COLUMN enhanced_auditing;

ALTER TABLE automated_task ADD COLUMN batch_size INTEGER;

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

ALTER TABLE court_case DELETE COLUMN version_label;
ALTER TABLE court_case ADD COLUMN is_data_anonymised          BOOLEAN                       NOT NULL;
ALTER TABLE court_case ADD COLUMN data_anonymised_by          INTEGER;
ALTER TABLE court_case ADD COLUMN data_anonymised_ts          TIMESTAMP WITH TIME ZONE;

ALTER TABLE daily_list DELETE COLUMN version_label;

ALTER TABLE event DELETE COLUMN event_name;
ALTER TABLE event DELETE COLUMN case_number;

ALTER TABLE media DELETE COLUMN ohr_id;
ALTER TABLE media DROP COLUMN hidden_by;
ALTER TABLE media DROP COLUMN hidden_ts;
ALTER TABLE media DROP COLUMN marked_for_manual_deletion;
ALTER TABLE media DROP COLUMN marked_for_manual_del_by;
ALTER TABLE media DROP COLUMN marked_for_manual_del_ts;
ALTER TABLE media DROP COLUMN case_number;

ALTER TABLE report DELETE COLUMN version_label;

ALTER TABLE transcription_document DELETE COLUMN ohr_id;
ALTER TABLE transcription_document DROP COLUMN hidden_by;
ALTER TABLE transcription_document DROP COLUMN hidden_ts;
ALTER TABLE transcription_document DROP COLUMN marked_for_manual_deletion;
ALTER TABLE transcription_document DROP COLUMN marked_for_manual_del_by;
ALTER TABLE transcription_document DROP COLUMN marked_for_manual_del_ts;

CREATE UNIQUE INDEX event_linked_case_pk ON event_linked_case(elc_id) TABLESPACE darts_indexes;
ALTER TABLE event_linked_case  ADD PRIMARY KEY USING INDEX event_linked_case_pk;

CREATE UNIQUE INDEX media_linked_case_pk ON media_linked_case(mlc_id) TABLESPACE darts_indexes;
ALTER TABLE media_linked_case  ADD PRIMARY KEY USING INDEX media_linked_case_pk;

CREATE UNIQUE INDEX object_admin_action_pk ON object_admin_action(oaa_id) TABLESPACE darts_indexes;
ALTER TABLE object_admin_action ADD PRIMARY KEY USING INDEX object_admin_action_pk;

CREATE UNIQUE INDEX object_retrieval_queue_pk ON object_retrieval_queue(orq_id) TABLESPACE darts_indexes;
ALTER TABLE object_retrieval_queue ADD PRIMARY KEY USING INDEX object_retrieval_queue_pk;


ALTER TABLE annotation_document DROP CONSTRAINT annotation_document_object_hidden_reason_fk;
ALTER TABLE annotation_document DROP CONSTRAINT annotation_document_hidden_by_fk;
ALTER TABLE annotation_document DROP CONSTRAINT annotation_document_marked_for_manual_del_by_fk;

ALTER TABLE case_document DROP CONSTRAINT case_document_object_hidden_reason_fk;
ALTER TABLE case_document DROP CONSTRAINT case_document_hidden_by_fk;
ALTER TABLE case_document DROP CONSTRAINT case_document_marked_for_manual_del_by_fk;

ALTER TABLE media DROP CONSTRAINT media_object_hidden_reason_fk;
ALTER TABLE media DROP CONSTRAINT media_hidden_by_fk;
ALTER TABLE media DROP CONSTRAINT media_marked_for_manual_del_by_fk;

ALTER TABLE transcription_document DROP CONSTRAINT transcription_document_object_hidden_reason_fk;
ALTER TABLE transcription_document DROP CONSTRAINT transcription_document_hidden_by_fk;
ALTER TABLE transcription_document DROP CONSTRAINT transcription_document_marked_for_manual_del_by_fk;
