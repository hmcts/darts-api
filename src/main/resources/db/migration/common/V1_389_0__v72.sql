-- v72 add tables arm_rpo_state, arm_rpo_status, arm_rpo_execution_detail, arm_automated_task

CREATE TABLE arm_rpo_state
(
    are_id          INTEGER           NOT NULL,
    are_description CHARACTER VARYING NOT NULL
);

COMMENT ON COLUMN arm_rpo_state.are_id
    IS 'primary key of arm_rpo_state';

CREATE SEQUENCE are_seq CACHE 20 START WITH 1000;


CREATE TABLE arm_rpo_status
(
    aru_id          INTEGER           NOT NULL,
    aru_description CHARACTER VARYING NOT NULL
);

COMMENT ON COLUMN arm_rpo_status.aru_id
    IS 'primary key of arm_rpo_status';

CREATE SEQUENCE aru_seq CACHE 20 START WITH 1000;


CREATE TABLE arm_rpo_execution_detail
(
    ard_id             INTEGER                  NOT NULL,
    are_id             INTEGER,
    aru_id             INTEGER,
    matter_id          CHARACTER VARYING,
    index_id           CHARACTER VARYING,
    entitlement_id     CHARACTER VARYING,
    storage_account_id CHARACTER VARYING,
    search_id          CHARACTER VARYING,
    production_id      CHARACTER VARYING,
    sorting_field      CHARACTER VARYING,
    search_item_count  INTEGER,
    created_ts         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         INTEGER                  NOT NULL,
    last_modified_ts   TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by   INTEGER                  NOT NULL
);

COMMENT ON COLUMN arm_rpo_execution_detail.ard_id
    IS 'primary key of arm_rpo_execution_detail';

COMMENT ON COLUMN arm_rpo_execution_detail.are_id
    IS 'foreign key to arm_rpo_state';

COMMENT ON COLUMN arm_rpo_execution_detail.aru_id
    IS 'foreign key to arm_rpo_status';

CREATE SEQUENCE ard_seq CACHE 20;


CREATE TABLE arm_automated_task
(
    aat_id              INTEGER NOT NULL,
    aut_id              INTEGER NOT NULL,
    rpo_csv_start_hour  INTEGER,
    rpo_csv_end_hour    INTEGER,
    arm_replay_start_ts TIMESTAMP WITH TIME ZONE,
    arm_replay_end_ts   TIMESTAMP WITH TIME ZONE,
    arm_attribute_type  CHARACTER VARYING
);

COMMENT ON COLUMN arm_automated_task.aat_id
    IS 'primary key of arm_automated_task';

COMMENT ON COLUMN arm_automated_task.aut_id
    IS 'foreign key to automated_task';

CREATE SEQUENCE aat_seq CACHE 20 START WITH 1000;


-- amend media_linked_case, with 3 new columns (source,created_ts/by)
ALTER TABLE media_linked_case
    ADD COLUMN source INTEGER DEFAULT 0;
ALTER TABLE media_linked_case
    ADD COLUMN created_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE media_linked_case
    ADD COLUMN created_by INTEGER;


-- amend court_case, with 2 new columns (case_object_name, folder_path)
ALTER TABLE court_case
    ADD COLUMN case_object_name CHARACTER VARYING(255);
ALTER TABLE court_case
    ADD COLUMN folder_path CHARACTER VARYING;


-- amend media, event, transcription with 1 new column, folder_path
ALTER TABLE media
    ADD COLUMN folder_path CHARACTER VARYING;
ALTER TABLE event
    ADD COLUMN folder_path CHARACTER VARYING;
ALTER TABLE transcription
    ADD COLUMN folder_path CHARACTER VARYING;


--    add table data_anonymisation
CREATE TABLE data_anonymisation
(
    dan_id            INTEGER                  NOT NULL,
    eve_id            INTEGER, -- only one of these must be populated, unenforced FK
    trc_id            INTEGER, -- only one of these must be populated, unenforced FK
    is_manual_request BOOLEAN,
    requested_by      INTEGER                  NOT NULL,
    requested_ts      TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_by       INTEGER,
    approved_ts       TIMESTAMP WITH TIME ZONE
);

COMMENT ON COLUMN data_anonymisation.dan_id
    IS 'primary key of data_anonymisation';

COMMENT ON COLUMN data_anonymisation.eve_id
    IS 'foreign key to event, unenforced to support deletion of event without reference to this table';

COMMENT ON COLUMN data_anonymisation.trc_id
    IS 'foreign key of transcription_comment, unenforce to support deletion of transcription_comment without reference to this table';

ALTER TABLE data_anonymisation
    ADD CONSTRAINT dan_one_of_eve_or_trc_nn
        CHECK (eve_id is not null or trc_id is not null);

CREATE SEQUENCE dan_seq CACHE 20;


--    add 2 columns to table external_object_directory
ALTER TABLE external_object_directory
    ADD COLUMN input_upload_processed_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE external_object_directory
    ADD COLUMN force_response_cleanup BOOLEAN;
