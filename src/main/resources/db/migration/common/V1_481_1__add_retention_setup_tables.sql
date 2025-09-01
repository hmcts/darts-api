--
-- flyway for retention preparation
--

CREATE TABLE IF NOT EXISTS cc_dets
(cas_id                        INTEGER
,case_object_id                CHARACTER VARYING(16)
,c_courthouse                  CHARACTER VARYING(64)
,c_case_id                     CHARACTER VARYING(32)
,c_closed_pre_live             INTEGER
,c_case_closed_date_pre_live   TIMESTAMP WITH TIME ZONE
,c_case_closed_date_crah       TIMESTAMP WITH TIME ZONE
,case_created_ts               TIMESTAMP WITH TIME ZONE
,cas_retention_fixed           CHARACTER VARYING(16)
,case_total_sentence           CHARACTER VARYING(16)
,retention_applies_from_ts     TIMESTAMP WITH TIME ZONE
,end_of_sentence_date_ts       TIMESTAMP WITH TIME ZONE
,manual_retention_override     INTEGER
,retain_until_ts               TIMESTAMP WITH TIME ZONE
,audio_folder_object_id        CHARACTER VARYING(16)
,case_closed_event_event_ts_by_pre_live_closed_date           TIMESTAMP WITH TIME ZONE
,case_closed_event_event_ts_eve_id_by_pre_live_closed_date    INTEGER
,case_closed_event_created_ts_by_pre_live_closed_date         TIMESTAMP WITH TIME ZONE
,case_closed_event_created_ts_eve_id_by_pre_live_closed_date  INTEGER
,case_closed_event_event_ts_by_audit_closed_date              TIMESTAMP WITH TIME ZONE
,case_closed_event_event_ts_eve_id_by_audit_closed_date       INTEGER
,case_closed_event_created_ts_by_audit_closed_date            TIMESTAMP WITH TIME ZONE
,case_closed_event_created_ts_eve_id_by_audit_closed_date     INTEGER
,case_closed_event_event_ts_case_closed_event_latest          TIMESTAMP WITH TIME ZONE
,case_closed_event_event_ts_eve_id_case_closed_event_latest   INTEGER
,case_closed_event_created_ts_case_closed_event_latest        TIMESTAMP WITH TIME ZONE
,case_closed_event_created_ts_eve_id_case_closed_event_latest INTEGER
,latest_event_by_event_ts_ts                                  TIMESTAMP WITH TIME ZONE
,latest_event_by_event_ts_eve_id                              INTEGER
,latest_event_by_created_ts                                   TIMESTAMP WITH TIME ZONE
,latest_event_by_created_ts_eve_id                            INTEGER
,latest_hearing_ended_event_by_event_ts                       TIMESTAMP WITH TIME ZONE
,latest_hearing_ended_event_by_event_ts_eve_id                INTEGER
,latest_hearing_ended_event_by_created_ts                     TIMESTAMP WITH TIME ZONE
,latest_hearing_ended_event_by_created_ts_eve_id              INTEGER
,latest_log_event_by_event_ts                                 TIMESTAMP WITH TIME ZONE
,latest_log_event_by_event_ts_eve_id                          INTEGER
,latest_log_event_by_created_ts                               TIMESTAMP WITH TIME ZONE
,latest_log_event_by_created_ts_eve_id                        INTEGER
,latest_sentencing_event_by_event_ts                          TIMESTAMP WITH TIME ZONE
,latest_sentencing_event_by_event_ts_eve_id                   INTEGER
,latest_sentencing_event_by_created_ts                        TIMESTAMP WITH TIME ZONE
,latest_sentencing_event_by_created_ts_eve_id                 INTEGER
,latest_sentencing_271_event_by_event_ts                      TIMESTAMP WITH TIME ZONE
,latest_sentencing_271_event_by_event_ts_eve_id               INTEGER
,latest_sentencing_271_event_by_created_ts                    TIMESTAMP WITH TIME ZONE
,latest_sentencing_271_event_by_created_ts_eve_id             INTEGER
,latest_media_by_created_ts                                   TIMESTAMP WITH TIME ZONE
,latest_media_by_created_ts_eve_id                            INTEGER
,latest_media_by_start_ts                                     TIMESTAMP WITH TIME ZONE
,latest_media_by_start_ts_eve_id                              INTEGER
,latest_media_by_end_ts                                       TIMESTAMP WITH TIME ZONE
,latest_media_by_end_ts_eve_id                                INTEGER
,latest_warrant_event_by_event_ts                             TIMESTAMP WITH TIME ZONE
,latest_warrant_event_by_event_ts_eve_id                      INTEGER
,latest_warrant_event_by_created_ts                           TIMESTAMP WITH TIME ZONE
,latest_warrant_event_by_created_ts_eve_id                    INTEGER
,ret_conf_score                                               INTEGER
,ret_conf_reason                                              CHARACTER VARYING(64)
,ret_conf_updated_ts                                          TIMESTAMP WITH TIME ZONE
,category_type                                                CHARACTER VARYING(32)
,latest_activity_period                                       CHARACTER VARYING(5)
);

CREATE TABLE IF NOT EXISTS cmr_dets
(cmd_id                        SERIAL
,cas_id                        INTEGER
,rpt_id                        INTEGER
,eve_id                        BIGINT
,total_sentence                CHARACTER VARYING(32)
);

CREATE TABLE IF NOT EXISTS cr_dets
(crd_id                        SERIAL
,cas_id                        INTEGER
,rpt_id                        INTEGER
,cmd_id                        INTEGER
,total_sentence                CHARACTER VARYING(32)
,retain_until_ts               TIMESTAMP WITH TIME ZONE
,retain_until_applied_on_ts    TIMESTAMP WITH TIME ZONE
,current_state                 CHARACTER VARYING(32)
,comments                      CHARACTER VARYING(150)
,confidence_category           INTEGER
,retention_object_id           CHARACTER VARYING(32)
,submitted_by                  INTEGER
,created_ts                    TIMESTAMP WITH TIME ZONE
,created_by                    INTEGER
,last_modified_ts              TIMESTAMP WITH TIME ZONE
,last_modified_by              INTEGER
);

CREATE TABLE IF NOT EXISTS wk_case_correction
(cas_id                        INTEGER
,category_type                 CHARACTER VARYING(32)
,case_closed_date_corr         BOOLEAN
,case_closed_corr              BOOLEAN
,retain_until_ts_corr          BOOLEAN
,new_case_closed               BOOLEAN
,new_closed_date_ts            TIMESTAMP WITH TIME ZONE
,case_old_closed_date_ts       TIMESTAMP WITH TIME ZONE
,case_audit_old_closed_date_ts TIMESTAMP WITH TIME ZONE
,closed_date_type              CHARACTER VARYING(50)
,eve_id                        BIGINT
,current_logic_rpt_id          INTEGER
,current_creation_rpt_id       INTEGER
,case_total_sentence           CHARACTER VARYING(16)
,new_retain_until_ts           TIMESTAMP WITH TIME ZONE
,retain_until_applied_on_ts    TIMESTAMP WITH TIME ZONE
,current_state                 CHARACTER VARYING(16)
,old_state                     CHARACTER VARYING(16)
,retention_object_id           CHARACTER VARYING(16)
,latest_activity_period        CHARACTER VARYING(5)
,best_closed_date_period       CHARACTER VARYING(5)
,adjusted_closed_date_ts       TIMESTAMP WITH TIME ZONE
,adjusted_closed_date_type     CHARACTER VARYING(50)
,adjusted_eve_id               BIGINT
,has_warrant                   BOOLEAN
,warrant_before_sentencing     BOOLEAN
,case_closed_before_sentencing BOOLEAN
);

CREATE INDEX IF NOT EXISTS ccd_ct_idx ON cc_dets(category_type);
CREATE INDEX IF NOT EXISTS wcc_ct_idx ON wk_case_correction(category_type);
