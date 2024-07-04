--v70 add default to is_data_anonymised on court_case
ALTER TABLE court_case
    ALTER COLUMN is_data_anonymised SET DEFAULT false;

--    add case_type, upload_priority to court_case
ALTER TABLE court_case
    ADD COLUMN case_type CHARACTER VARYING;
ALTER TABLE court_case
    ADD COLUMN upload_priority INTEGER;

--    move confidence* columns from case_overflow to court_case and prefix with ret
ALTER TABLE court_case
    ADD COLUMN ret_conf_level INTEGER;
ALTER TABLE court_case
    ADD COLUMN ret_conf_reason CHARACTER VARYING;

UPDATE court_case
SET ret_conf_level  = confidence_level,
    ret_conf_reason = confidence_reason
FROM case_overflow
WHERE case_overflow.cas_id = court_case.cas_id;

ALTER TABLE case_overflow
    DROP COLUMN confidence_level;
ALTER TABLE case_overflow
    DROP COLUMN confidence_reason;

--    add case_object_name to case_overflow to support migration from dm_sysobject.object_name
ALTER TABLE case_overflow
    ADD COLUMN case_object_name CHARACTER VARYING(255);

--    add event_status and is_current to event
ALTER TABLE event
    ADD COLUMN event_status CHARACTER VARYING;
ALTER TABLE event
    ADD COLUMN is_current BOOLEAN NOT NULL DEFAULT true;

--    make courthouse and case_number nullable on event_linked_case and media_linked_case
-- Note: These fields already permit null, so no action taken.

--    amend datatypes of courthouse and case_number on both tables, to match modernised
ALTER TABLE event_linked_case
    ALTER COLUMN courthouse_name TYPE CHARACTER VARYING;
ALTER TABLE event_linked_case
    ALTER COLUMN case_number TYPE CHARACTER VARYING;

ALTER TABLE media_linked_case
    ALTER COLUMN courthouse_name TYPE CHARACTER VARYING;
ALTER TABLE media_linked_case
    ALTER COLUMN case_number TYPE CHARACTER VARYING;

--    remove judge_hearing_date from hearing
ALTER TABLE hearing
    DROP COLUMN judge_hearing_date;

--    remove reference_id from media
ALTER TABLE media
    DROP COLUMN reference_id;

--    add transcription_object_name to transcription
ALTER TABLE transcription
    ADD COLUMN transcription_object_name CHARACTER VARYING(255);

--    replace requestor string to requested_by on transcription with fk to user_account
ALTER TABLE transcription
    DROP COLUMN requestor;

ALTER TABLE transcription
    ADD COLUMN requested_by INTEGER;
COMMENT ON COLUMN transcription.requested_by IS 'foreign key from user_account, corresponding to moj_transcription_s.c_requestor';

ALTER TABLE transcription
    ADD CONSTRAINT transcription_requested_by_fk FOREIGN KEY (requested_by) REFERENCES user_account (usr_id);

--    add is_migrated to transcription_comment
ALTER TABLE transcription_comment
    ADD COLUMN is_migrated BOOLEAN NOT NULL DEFAULT false;

--    re-order list of optional FK columns in external_object_directory
--    add multi-column constraint on same
ALTER TABLE external_object_directory
    ADD CONSTRAINT eod_one_of_ado_cad_med_trd_nn
        CHECK (ado_id is not null or cad_id is not null or med_id is not null or trd_id is not null);

--    add similar multi-column constraints to event_linked_case, media_linked_case, object_admin_action, object_retrieval_queue
ALTER TABLE event_linked_case
    ADD CONSTRAINT elc_modern_or_legacy_case_nn
        CHECK ((cas_id is not null) or (courthouse_name is not null and case_number is not null));

ALTER TABLE media_linked_case
    ADD CONSTRAINT mlc_modern_or_legacy_case_nn
        CHECK ((cas_id is not null) or (courthouse_name is not null and case_number is not null));

ALTER TABLE object_admin_action
    ADD CONSTRAINT oaa_one_of_ado_cad_med_trd_nn
        CHECK (ado_id is not null or cad_id is not null or med_id is not null or trd_id is not null);

ALTER TABLE object_retrieval_queue
    ADD CONSTRAINT orq_one_of_med_or_trd_nn
        CHECK (med_id is not null or trd_id is not null);
