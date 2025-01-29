CREATE TABLE rps_retainer
(
    rpr_id                         INTEGER                  NOT NULL,
    cas_id                         INTEGER,
    rpt_id                         INTEGER                  NOT NULL,
    rps_retainer_object_id         CHARACTER VARYING        NOT NULL,
    is_current                     BOOLEAN,
    dm_retainer_root_id            CHARACTER VARYING,
    dm_retention_rule_type         INTEGER,
    dm_retention_date              TIMESTAMP WITH TIME ZONE,
    dmc_current_phase_id           CHARACTER VARYING,
    dmc_entry_date                 TIMESTAMP WITH TIME ZONE,
    dmc_parent_ancestor_id         CHARACTER VARYING,
    dmc_phase_name                 CHARACTER VARYING,
    dmc_qualification_date         TIMESTAMP WITH TIME ZONE,
    dmc_retention_base_date        TIMESTAMP WITH TIME ZONE,
    dmc_retention_policy_id        CHARACTER VARYING,
    dmc_ultimate_ancestor_id       CHARACTER VARYING,
    dmc_vdm_retention_rule         INTEGER,
    dmc_is_superseded              INTEGER,
    dmc_superseded_date            TIMESTAMP WITH TIME ZONE,
    dmc_superseded_phase_id        CHARACTER VARYING,
    dmc_snapshot_retention_rule    INTEGER,
    dmc_approval_required          INTEGER,
    dmc_approval_status            CHARACTER VARYING,
    dmc_approved_date              TIMESTAMP WITH TIME ZONE,
    dmc_projected_disposition_date TIMESTAMP WITH TIME ZONE,
    dmc_is_qualification_suspended INTEGER,
    dmc_suspension_lift_date       TIMESTAMP WITH TIME ZONE,
    dmc_base_date_override         TIMESTAMP WITH TIME ZONE,
    dms_object_name                CHARACTER VARYING,
    dms_i_chronicle_id             CHARACTER VARYING,
    dms_r_policy_id                CHARACTER VARYING,
    dms_r_resume_state             INTEGER,
    dms_r_current_state            INTEGER,
    created_ts                     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                     INTEGER                  NOT NULL,
    last_modified_ts               TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by               INTEGER                  NOT NULL
);

ALTER TABLE rps_retainer
    ADD CONSTRAINT rps_retainer_court_case_fk
        FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE rps_retainer
    ADD CONSTRAINT rps_retainer_retention_policy_type_fk
        FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE rps_retainer
    ADD CONSTRAINT rps_retainer_created_by_fk
        FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE rps_retainer
    ADD CONSTRAINT rps_retainer_last_modified_by_fk
        FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

CREATE SEQUENCE rpr_seq CACHE 20;

COMMENT ON TABLE rps_retainer
    IS 'is essentially a legacy table, based on the component tables necessary to derive the dmc_rps_retainer object';
COMMENT ON COLUMN rps_retainer.rpr_id
    IS 'primary key of case_rps_retainer';
COMMENT ON COLUMN rps_retainer.cas_id
    IS 'foreign key to court_case';



ALTER TABLE case_retention_audit_heritage
    DROP COLUMN r_object_id;

ALTER TABLE case_retention_audit_heritage
    ADD COLUMN rah_id INTEGER NOT NULL; -- TODO DOES THIS NEED A DEFAULT?!

ALTER TABLE case_retention_audit_heritage
    ADD COLUMN cas_id INTEGER;

ALTER TABLE case_retention_audit_heritage
    ADD COLUMN rpt_id INTEGER;

ALTER TABLE case_retention_audit_heritage
    ADD COLUMN case_retention_audit_object_id CHARACTER VARYING(16) NOT NULL;

ALTER TABLE case_retention_audit_heritage
    ADD COLUMN is_current BOOLEAN;

ALTER TABLE case_retention_audit_heritage
    ALTER COLUMN c_username TYPE INTEGER USING c_username::integer;

ALTER TABLE case_retention_audit_heritage
    ALTER COLUMN r_creator_name TYPE INTEGER USING c_username::integer;

ALTER TABLE case_retention_audit_heritage
    ALTER COLUMN r_modifier TYPE INTEGER USING c_username::integer;

ALTER TABLE case_retention_audit_heritage
    ALTER COLUMN owner_name TYPE INTEGER USING c_username::integer;

ALTER TABLE case_retention_audit_heritage
    ADD CONSTRAINT case_retention_audit_heritage_court_case_fk
        FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_retention_audit_heritage
    ADD CONSTRAINT case_retention_audit_heritage_retention_policy_type_fk
        FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE case_retention_audit_heritage
    ADD CONSTRAINT case_retention_audit_heritage_c_username_fk
        FOREIGN KEY (c_username) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_audit_heritage
    ADD CONSTRAINT case_retention_audit_heritage_r_creator_name_fk
        FOREIGN KEY (r_creator_name) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_audit_heritage
    ADD CONSTRAINT case_retention_audit_heritage_r_modifier_fk
        FOREIGN KEY (r_modifier) REFERENCES user_account(usr_id);

ALTER TABLE case_retention_audit_heritage
    ADD CONSTRAINT case_retention_audit_heritage_owner_name_fk
        FOREIGN KEY (owner_name) REFERENCES user_account(usr_id);



ALTER TABLE case_overflow
    DROP COLUMN is_standard_policy;

ALTER TABLE case_overflow
    DROP COLUMN is_permanent_policy;

ALTER TABLE case_overflow
    DROP COLUMN checked_ts;

ALTER TABLE case_overflow
    DROP COLUMN corrected_ts;

ALTER TABLE case_overflow
    DROP COLUMN case_object_name;

ALTER TABLE case_overflow
    ADD COLUMN rpt_id INTEGER;

ALTER TABLE case_overflow
    ADD COLUMN case_created_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_overflow
    ADD COLUMN case_last_modified_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_overflow
    ADD COLUMN audio_last_modified_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_overflow
    ADD COLUMN created_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_overflow
    ADD COLUMN last_modified_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE case_overflow
    ADD CONSTRAINT case_overflow_retention_policy_type_fk
        FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);



CREATE TABLE case_retention_extra
(
    cas_id                        INTEGER                  NOT NULL,
    current_rah_id                INTEGER,
    current_rah_rpt_id            INTEGER,
    current_rpr_id                INTEGER,
    current_rpr_rpt_id            INTEGER,
    retention_fixed_rpt_id        INTEGER,
    case_total_sentence           CHARACTER VARYING,
    case_retention_fixed          CHARACTER VARYING,
    end_of_sentence_date_ts       TIMESTAMP WITH TIME ZONE,
    manual_retention_override     INTEGER,
    actual_case_closed_flag       INTEGER,
    actual_case_closed_ts         TIMESTAMP WITH TIME ZONE,
    actual_retain_until_ts        TIMESTAMP WITH TIME ZONE,
    actual_case_created_ts        TIMESTAMP WITH TIME ZONE,
    submitted_by                  INTEGER,
    rps_retainer_object_id        CHARACTER VARYING,
    case_closed_eve_id            INTEGER,
    case_closed_event_ts          TIMESTAMP WITH TIME ZONE,
    max_event_ts                  TIMESTAMP WITH TIME ZONE,
    max_media_ts                  TIMESTAMP WITH TIME ZONE,
    closure_method_type           CHARACTER VARYING,
    best_case_closed_ts           TIMESTAMP WITH TIME ZONE,
    best_case_closed_type         CHARACTER VARYING,
    best_retainer_retain_until_ts TIMESTAMP WITH TIME ZONE,
    best_audit_retain_until_ts    TIMESTAMP WITH TIME ZONE,
    retention_aged_policy_name    CHARACTER VARYING,
    case_closed_diff_in_days      INTEGER,
    r_retain_until_diff_in_days   INTEGER,
    a_retain_until_diff_in_days   INTEGER,
    validation_error_1            CHARACTER VARYING,
    validation_error_2            CHARACTER VARYING,
    validation_error_3            CHARACTER VARYING,
    validation_error_4            CHARACTER VARYING,
    validation_error_5            CHARACTER VARYING,
    ret_conf_score                INTEGER,
    ret_conf_reason               CHARACTER VARYING,
    ret_conf_updated_ts           TIMESTAMP WITH TIME ZONE,
    validated_ts                  TIMESTAMP WITH TIME ZONE,
    created_ts                    TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_ts              TIMESTAMP WITH TIME ZONE NOT NULL,
    migrated_ts                   TIMESTAMP WITH TIME ZONE
);

ALTER TABLE case_retention_extra
    ADD CONSTRAINT case_retention_extra_court_case_fk
        FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

CREATE SEQUENCE rah_seq CACHE 20;



CREATE TABLE retention_policy_type_heritage_mapping
(
    rhm_id               INTEGER           NOT NULL,
    heritage_policy_name CHARACTER VARYING NOT NULL,
    heritage_table       CHARACTER VARYING NOT NULL,
    modernised_rpt_id    INTEGER           NOT NULL
);

CREATE SEQUENCE rhm_seq CACHE 20;
