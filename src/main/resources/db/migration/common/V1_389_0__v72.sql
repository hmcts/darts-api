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
    ADD COLUMN created_ts TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE media_linked_case
    ADD COLUMN created_by INTEGER NOT NULL DEFAULT 0;


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
