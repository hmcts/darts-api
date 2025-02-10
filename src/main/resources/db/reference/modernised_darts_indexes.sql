--modernised_pg_default
--
-- v1 aligned to v60 of the modernised schema, v19 security, v6 retention
--    commented statements, unnecessary due to presence of PK, which will support the FK in question.
-- v2 additional indexes to accommodate initial access to driving site table
-- adding storage clauses to all indexes
-- v3 remove indexes on hea_id and cas_id from transcription
-- v4 amend a number of the indexes on character columns to be case-insenstive upper() function based
-- v5 add 2 user_account indexes
-- v6 add 3 application team derived indexes
-- v7 amend all tablespaces to pg_default
-- v8 add trigram indexes for pattern search suppt
-- v9 add 2 event and 2 media indexes
-- v10 amend index on user_account from user_name to user_full_name
-- v11 add numerous indexes to accommodate all FKs
-- v12 add index on media for chronicle_id

SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

-- v60 application
CREATE INDEX ann_cur_own_fk     ON ANNOTATION(current_owner) TABLESPACE pg_default;
CREATE INDEX ann_cre_by_fk      ON ANNOTATION(created_by) TABLESPACE pg_default;
CREATE INDEX ann_lst_mod_by_fk  ON ANNOTATION(last_modified_by) TABLESPACE pg_default;
CREATE INDEX ann_del_by_fk      ON ANNOTATION(deleted_by) TABLESPACE pg_default;

CREATE INDEX ado_ann_fk         ON ANNOTATION_DOCUMENT(ann_id) TABLESPACE pg_default;
CREATE INDEX ado_del_by_fk      ON ANNOTATION_DOCUMENT(deleted_by) TABLESPACE pg_default;
CREATE INDEX ado_last_mod_by_fk ON ANNOTATION_DOCUMENT(last_modified_by) TABLESPACE pg_default;
CREATE INDEX ado_up_by_fk       ON ANNOTATION_DOCUMENT(uploaded_by) TABLESPACE pg_default;

CREATE INDEX aat_aut_fk         ON ARM_AUTOMATED_TASK(aut_id) TABLESPACE pg_default;

CREATE INDEX ard_are_fk         ON ARM_RPO_EXECUTION_DETAIL(are_id) TABLESPACE pg_default;
CREATE INDEX ard_aru_fk         ON ARM_RPO_EXECUTION_DETAIL(aru_id) TABLESPACE pg_default;
CREATE INDEX ard_cre_by_fk      ON ARM_RPO_EXECUTION_DETAIL(created_by) TABLESPACE pg_default;
CREATE INDEX ard_lst_mod_by_fk  ON ARM_RPO_EXECUTION_DETAIL(last_modified_by) TABLESPACE pg_default;

CREATE INDEX aud_cas_fk         ON AUDIT(cas_id) TABLESPACE pg_default;
CREATE INDEX aud_aua_fk         ON AUDIT(aua_id) TABLESPACE pg_default;
CREATE INDEX aud_usr_fk         ON AUDIT(usr_id) TABLESPACE pg_default;
CREATE INDEX aud_cre_by_fk      ON AUDIT(created_by) TABLESPACE pg_default;
CREATE INDEX aud_lst_mod_by_fk  ON AUDIT(last_modified_by) TABLESPACE pg_default;

CREATE INDEX aua_cre_by_fk      ON AUDIT_ACTIVITY(created_by) TABLESPACE pg_default;
CREATE INDEX aua_lst_mod_by_fk  ON AUDIT_ACTIVITY(last_modified_by) TABLESPACE pg_default;

CREATE INDEX aut_cre_by_fk      ON AUTOMATED_TASK(created_by) TABLESPACE pg_default;
CREATE INDEX aut_lst_mod_by_fk  ON AUTOMATED_TASK(last_modified_by) TABLESPACE pg_default;

CREATE INDEX cad_cas_fk         ON CASE_DOCUMENT(cas_id) TABLESPACE pg_default;
CREATE INDEX cad_cre_by_fk      ON CASE_DOCUMENT(created_by) TABLESPACE pg_default;
CREATE INDEX cad_lst_mod_by_fk  ON CASE_DOCUMENT(last_modified_by) TABLESPACE pg_default;
CREATE INDEX cad_del_by_fk      ON CASE_DOCUMENT(deleted_by) TABLESPACE pg_default;

--CREATE INDEX caj_cas_fk         ON CASE_JUDGE_AE(cas_id);  
CREATE INDEX caj_jud_fk         ON CASE_JUDGE_AE(jud_id) TABLESPACE pg_default;

CREATE INDEX caj_tra_fk         ON CASE_TRANSCRIPTION_AE(tra_id) TABLESPACE pg_default;

CREATE INDEX cas_evh_fk         ON COURT_CASE(evh_id) TABLESPACE pg_default;
CREATE INDEX cas_cth_fk         ON COURT_CASE(cth_id) TABLESPACE pg_default;
CREATE INDEX cas_cre_by_fk      ON COURT_CASE(created_by) TABLESPACE pg_default;
CREATE INDEX cas_lst_mod_by_fk  ON COURT_CASE(last_modified_by) TABLESPACE pg_default;
CREATE INDEX cas_dat_anon_by_fk ON COURT_CASE(data_anonymised_by) TABLESPACE pg_default;
CREATE INDEX cas_del_by_fk      ON COURT_CASE(deleted_by) TABLESPACE pg_default;

CREATE INDEX cth_cre_by_fk      ON COURTHOUSE(created_by) TABLESPACE pg_default;
CREATE INDEX cth_lst_mod_by_fk  ON COURTHOUSE(last_modified_by) TABLESPACE pg_default;

--CREATE INDEX cra_cth_fk         ON COURTHOUSE_REGION_AE(cth_id);
CREATE INDEX cra_reg_fk         ON COURTHOUSE_REGION_AE(reg_id) TABLESPACE pg_default;

CREATE INDEX ctr_cth_fk         ON COURTROOM(cth_id) TABLESPACE pg_default;
CREATE INDEX ctr_cre_by_fk      ON COURTROOM(created_by) TABLESPACE pg_default;

CREATE INDEX dan_eve_fk         ON DATA_ANONYMISATION(eve_id) TABLESPACE pg_default;
CREATE INDEX dan_trc_fk         ON DATA_ANONYMISATION(trc_id) TABLESPACE pg_default;
CREATE INDEX dan_req_by_fk      ON DATA_ANONYMISATION(requested_by) TABLESPACE pg_default;
CREATE INDEX dan_apprv_by_fk    ON DATA_ANONYMISATION(approved_by) TABLESPACE pg_default;

CREATE INDEX dal_elt_fk         ON DAILY_LIST(elt_id) TABLESPACE pg_default;
CREATE INDEX dal_cre_by_fk      ON DAILY_LIST(created_by) TABLESPACE pg_default;
CREATE INDEX dal_lst_mod_by     ON DAILY_LIST(last_modified_by) TABLESPACE pg_default;

CREATE INDEX dfc_cas_fk         ON DEFENCE(cas_id) TABLESPACE pg_default;
CREATE INDEX dfc_cre_by_fk      ON DEFENCE(created_by) TABLESPACE pg_default;
CREATE INDEX dfc_lst_mod_by_fk  ON DEFENCE(last_modified_by) TABLESPACE pg_default;

CREATE INDEX dfd_cas_fk         ON DEFENDANT(cas_id) TABLESPACE pg_default;
CREATE INDEX dfd_cre_by_fk      ON DEFENDANT(created_by) TABLESPACE pg_default;
CREATE INDEX dfd_lst_mod_by_fk  ON DEFENDANT(last_modified_by) TABLESPACE pg_default;

CREATE INDEX eve_ctr_fk         ON EVENT(ctr_id) TABLESPACE pg_default;
CREATE INDEX eve_evh_fk         ON EVENT(evh_id) TABLESPACE pg_default;
CREATE INDEX eve_cre_by_fk      ON EVENT(created_by) TABLESPACE pg_default;
CREATE INDEX eve_lst_mod_by_fk  ON EVENT(last_modified_by) TABLESPACE pg_default;

CREATE INDEX evh_cre_by_fk      ON EVENT_HANDLER(created_by) TABLESPACE pg_default;

CREATE INDEX elc_eve_fk         ON EVENT_LINKED_CASE(eve_id) TABLESPACE pg_default;
CREATE INDEX elc_cas_fk         ON EVENT_LINKED_CASE(cas_id) TABLESPACE pg_default;

CREATE INDEX eod_med_fk         ON EXTERNAL_OBJECT_DIRECTORY(med_id) TABLESPACE pg_default;
CREATE INDEX eod_trd_fk         ON EXTERNAL_OBJECT_DIRECTORY(trd_id) TABLESPACE pg_default;
CREATE INDEX eod_cad_fk         ON EXTERNAL_OBJECT_DIRECTORY(cad_id) TABLESPACE pg_default;
CREATE INDEX eod_ado_fk         ON EXTERNAL_OBJECT_DIRECTORY(ado_id) TABLESPACE pg_default;
CREATE INDEX eod_cre_by_fk      ON EXTERNAL_OBJECT_DIRECTORY(created_by) TABLESPACE pg_default;
CREATE INDEX eod_lst_mod_by_fk  ON EXTERNAL_OBJECT_DIRECTORY(last_modified_by) TABLESPACE pg_default;
CREATE INDEX eod_ors_fk         ON EXTERNAL_OBJECT_DIRECTORY(ors_id) TABLESPACE pg_default;
CREATE INDEX eod_elt_fk         ON EXTERNAL_OBJECT_DIRECTORY(elt_id) TABLESPACE pg_default;

CREATE INDEX esa_cre_by_fk      ON EXTERNAL_SERVICE_AUTH_TOKEN(created_by) TABLESPACE pg_default;
CREATE INDEX esa_lst_mod_by_fk  ON EXTERNAL_SERVICE_AUTH_TOKEN(last_modified_by) TABLESPACE pg_default;

CREATE INDEX hea_cas_fk         ON HEARING(cas_id) TABLESPACE pg_default;
CREATE INDEX hea_ctr_fk         ON HEARING(ctr_id) TABLESPACE pg_default;
CREATE INDEX hea_cre_by_fk      ON HEARING(created_by) TABLESPACE pg_default;
CREATE INDEX hea_lst_mod_by_fk  ON HEARING(last_modified_by) TABLESPACE pg_default;

--CREATE INDEX haa_hea_fk         ON HEARING_ANNOTATION_AE(hea_id);
CREATE INDEX haa_ann_fk         ON HEARING_ANNOTATION_AE(ann_id) TABLESPACE pg_default;

--CREATE INDEX hee_hea_fk         ON HEARING_EVENT_AE(hea_id);
CREATE INDEX hee_eve_fk         ON HEARING_EVENT_AE(eve_id) TABLESPACE pg_default;

--CREATE INDEX hej_hea_fk         ON HEARING_JUDGE_AE(hea_id);
CREATE INDEX hej_jud_fk         ON HEARING_JUDGE_AE(jud_id) TABLESPACE pg_default;

--CREATE INDEX hem_hea_fk         ON HEARING_MEDIA_AE(hea_id);
CREATE INDEX hem_med_fk         ON HEARING_MEDIA_AE(med_id) TABLESPACE pg_default;

CREATE INDEX het_tra_fk         ON HEARING_TRANSCRIPTION_AE(tra_id) TABLESPACE pg_default;

CREATE INDEX jud_cre_by_fk      ON JUDGE(created_by) TABLESPACE pg_default;
CREATE INDEX jud_lst_mod_by_fk  ON JUDGE(last_modified_by) TABLESPACE pg_default;

CREATE INDEX med_ctr_id         ON MEDIA(ctr_id) TABLESPACE pg_default;
CREATE INDEX med_cre_by_fk      ON MEDIA(created_by) TABLESPACE pg_default;
CREATE INDEX med_lst_mod_by_fk  ON MEDIA(last_modified_by) TABLESPACE pg_default;
CREATE INDEX med_del_by_fk      ON MEDIA(deleted_by) TABLESPACE pg_default;

CREATE INDEX mlc_med_fk         ON MEDIA_LINKED_CASE(med_id) TABLESPACE pg_default;
CREATE INDEX mlc_cas_fk         ON MEDIA_LINKED_CASE(cas_id) TABLESPACE pg_default;
CREATE INDEX mlc_cre_by_fk      ON MEDIA_LINKED_CASE(created_by) TABLESPACE pg_default;

CREATE INDEX mer_hea_fk         ON MEDIA_REQUEST(hea_id) TABLESPACE pg_default;
CREATE INDEX mer_cre_by_fk      ON MEDIA_REQUEST(created_by) TABLESPACE pg_default;
CREATE INDEX mer_lst_mod_by_fk  ON MEDIA_REQUEST(last_modified_by) TABLESPACE pg_default;
CREATE INDEX mer_req_fk         ON MEDIA_REQUEST(requestor) TABLESPACE pg_default;
CREATE INDEX mer_cur_own_fk     ON MEDIA_REQUEST(current_owner) TABLESPACE pg_default;

CREATE INDEX nod_ctr_fk         ON NODE_REGISTER(ctr_id) TABLESPACE pg_default;
CREATE INDEX nod_cre_by_fk      ON NODE_REGISTER(created_by) TABLESPACE pg_default;

CREATE INDEX not_cas_fk         ON NOTIFICATION(cas_id) TABLESPACE pg_default;
CREATE INDEX not_cre_by_fk      ON NOTIFICATION(created_by) TABLESPACE pg_default;
CREATE INDEX not_lst_mod_fk     ON NOTIFICATION(last_modified_by) TABLESPACE pg_default;

CREATE INDEX oaa_ado_fk         ON OBJECT_ADMIN_ACTION(ado_id) TABLESPACE pg_default;
CREATE INDEX oaa_cad_fk         ON OBJECT_ADMIN_ACTION(cad_id) TABLESPACE pg_default;
CREATE INDEX oaa_med_fk         ON OBJECT_ADMIN_ACTION(med_id) TABLESPACE pg_default;
CREATE INDEX oaa_trd_fk         ON OBJECT_ADMIN_ACTION(trd_id) TABLESPACE pg_default;
CREATE INDEX oaa_ohr_fk         ON OBJECT_ADMIN_ACTION(ohr_id) TABLESPACE pg_default;
CREATE INDEX oaa_hid_by_fk      ON OBJECT_ADMIN_ACTION(hidden_by) TABLESPACE pg_default;
CREATE INDEX oaa_mkd_del_by_fk  ON OBJECT_ADMIN_ACTION(marked_for_manual_del_by) TABLESPACE pg_default;

CREATE INDEX orq_med_fk         ON OBJECT_RETRIEVAL_QUEUE(med_id) TABLESPACE pg_default;
CREATE INDEX orq_trd_fk         ON OBJECT_RETRIEVAL_QUEUE(trd_id) TABLESPACE pg_default;
CREATE INDEX orq_cre_by_fk      ON OBJECT_RETRIEVAL_QUEUE(created_by) TABLESPACE pg_default;
CREATE INDEX orq_lst_mod_by_fk  ON OBJECT_RETRIEVAL_QUEUE(last_modified_by) TABLESPACE pg_default;

CREATE INDEX prn_cas_fk         ON PROSECUTOR(cas_id) TABLESPACE pg_default;
CREATE INDEX prn_cre_by_fk      ON PROSECUTOR(created_by) TABLESPACE pg_default;
CREATE INDEX prn_lst_mod_by_fk  ON PROSECUTOR(last_modified_by) TABLESPACE pg_default;

CREATE INDEX rep_cre_by_fk      ON REPORT(created_by) TABLESPACE pg_default;
CREATE INDEX rep_lst_mod_by_fk  ON REPORT(last_modified_by) TABLESPACE pg_default;

--CREATE INDEX tra_cas_fk         ON TRANSCRIPTION(cas_id) TABLESPACE pg_default;
CREATE INDEX tra_ctr_fk         ON TRANSCRIPTION(ctr_id) TABLESPACE pg_default;
CREATE INDEX tra_trs_fk         ON TRANSCRIPTION(trs_id) TABLESPACE pg_default;
CREATE INDEX tra_tru_fk         ON TRANSCRIPTION(tru_id) TABLESPACE pg_default;
--CREATE INDEX tra_hea_fk         ON TRANSCRIPTION(hea_id) TABLESPACE pg_default;
CREATE INDEX tra_cre_by_fk      ON TRANSCRIPTION(created_by) TABLESPACE pg_default;
CREATE INDEX tra_lst_mod_by_fk  ON TRANSCRIPTION(last_modified_by) TABLESPACE pg_default;
CREATE INDEX tra_trt_fk         ON TRANSCRIPTION(trt_id) TABLESPACE pg_default;
CREATE INDEX tra_del_by_fk      ON TRANSCRIPTION(deleted_by) TABLESPACE pg_default;
CREATE INDEX tra_req_by_fk      ON TRANSCRIPTION(requested_by) TABLESPACE pg_default;

CREATE INDEX trc_tra_fk         ON TRANSCRIPTION_COMMENT(tra_id) TABLESPACE pg_default;
CREATE INDEX trc_trw_fk         ON TRANSCRIPTION_COMMENT(trw_id) TABLESPACE pg_default;
CREATE INDEX trc_auth_fk        ON TRANSCRIPTION_COMMENT(author) TABLESPACE pg_default;
CREATE INDEX trc_cre_by_fk      ON TRANSCRIPTION_COMMENT(created_by) TABLESPACE pg_default;
CREATE INDEX trc_lst_mod_by_fk  ON TRANSCRIPTION_COMMENT(last_modified_by) TABLESPACE pg_default;

CREATE INDEX trd_tra_fk         ON TRANSCRIPTION_DOCUMENT(tra_id) TABLESPACE pg_default;
CREATE INDEX trd_del_by_fk      ON TRANSCRIPTION_DOCUMENT(deleted_by) TABLESPACE pg_default;
CREATE INDEX trd_upl_by_fk      ON TRANSCRIPTION_DOCUMENT(uploaded_by) TABLESPACE pg_default;
CREATE INDEX trd_lst_mod_by_fk  ON TRANSCRIPTION_DOCUMENT(last_modified_by) TABLESPACE pg_default;

CREATE INDEX tlc_tra_fk         ON TRANSCRIPTION_LINKED_CASE(tra_id) TABLESPACE pg_default;
CREATE INDEX tlc_cas_fk         ON TRANSCRIPTION_LINKED_CASE(cas_id) TABLESPACE pg_default;

CREATE INDEX trw_tra_fk         ON TRANSCRIPTION_WORKFLOW(tra_id) TABLESPACE pg_default;
CREATE INDEX trw_trs_fk         ON TRANSCRIPTION_WORKFLOW(trs_id) TABLESPACE pg_default;
CREATE INDEX trw_wrkflw_act_fk  ON TRANSCRIPTION_WORKFLOW(workflow_actor) TABLESPACE pg_default;

CREATE INDEX trm_mer_fk         ON TRANSFORMED_MEDIA(mer_id) TABLESPACE pg_default;
CREATE INDEX trm_cre_by_fk      ON TRANSFORMED_MEDIA(created_by) TABLESPACE pg_default;
CREATE INDEX trm_lst_mod_by_fk  ON TRANSFORMED_MEDIA(last_modified_by) TABLESPACE pg_default;

CREATE INDEX tod_cre_by_fk      ON TRANSIENT_OBJECT_DIRECTORY(created_by) TABLESPACE pg_default;
CREATE INDEX tod_lst_mod_by_fk  ON TRANSIENT_OBJECT_DIRECTORY(last_modified_by) TABLESPACE pg_default;
CREATE INDEX tod_trm_fk         ON TRANSIENT_OBJECT_DIRECTORY(trm_id) TABLESPACE pg_default;
CREATE INDEX tod_ors_fk         ON TRANSIENT_OBJECT_DIRECTORY(ors_id) TABLESPACE pg_default;

--v19 security
CREATE INDEX grp_rol_fk         ON SECURITY_GROUP(rol_id) TABLESPACE pg_default;
CREATE INDEX grp_cre_by_fk      ON SECURITY_GROUP(created_by) TABLESPACE pg_default;
CREATE INDEX grp_lst_mod_by_fk  ON SECURITY_GROUP(last_modified_by) TABLESPACE pg_default;

--CREATE INDEX grc_grp_fk         ON SECURITY_GROUP_COURTHOUSE_AE(grp_id);
CREATE INDEX grc_cth_fk         ON SECURITY_GROUP_COURTHOUSE_AE(cth_id) TABLESPACE pg_default;

CREATE INDEX gua_grp_fk         ON SECURITY_GROUP_USER_ACCOUNT_AE(grp_id) TABLESPACE pg_default;
--CREATE INDEX gua_usr_fk         ON SECURITY_GROUP_USER_ACCOUNT_AE(usr_id);

--CREATE INDEX rop_rol_fk         ON SECURITY_ROLE_PERMISSION_AE(rol_id);
CREATE INDEX rop_per_fk         ON SECURITY_ROLE_PERMISSION_AE(per_id) TABLESPACE pg_default;

--v6 retention
CREATE INDEX car_cas_fk         ON CASE_RETENTION(cas_id) TABLESPACE pg_default;
CREATE INDEX car_rpt_fk         ON CASE_RETENTION(rpt_id) TABLESPACE pg_default;
CREATE INDEX car_cre_by_fk      ON CASE_RETENTION(created_by) TABLESPACE pg_default;
CREATE INDEX car_lst_mod_by_fk  ON CASE_RETENTION(last_modified_by) TABLESPACE pg_default;
CREATE INDEX car_sub_by_fk      ON CASE_RETENTION(submitted_by) TABLESPACE pg_default;
CREATE INDEX car_cmr_fk         ON CASE_RETENTION(cmr_id) TABLESPACE pg_default;

CREATE INDEX cmr_cas_fk         ON CASE_MANAGEMENT_RETENTION(cas_id) TABLESPACE pg_default;
CREATE INDEX cmr_rpt_fk         ON CASE_MANAGEMENT_RETENTION(rpt_id) TABLESPACE pg_default;
CREATE INDEX cmr_eve_fk         ON CASE_MANAGEMENT_RETENTION(eve_id) TABLESPACE pg_default;

CREATE INDEX rpt_cre_by_fk      ON RETENTION_POLICY_TYPE(created_by) TABLESPACE pg_default;
CREATE INDEX rpt_lst_mod_by_fk  ON RETENTION_POLICY_TYPE(last_modified_by) TABLESPACE pg_default;

CREATE INDEX rpr_cas_fk         ON RPS_RETAINER(cas_id) TABLESPACE pg_default;
CREATE INDEX rpr_rpt_fk         ON RPS_RETAINER(rpt_id) TABLESPACE pg_default;
CREATE INDEX rpr_cre_by_fk      ON RPS_RETAINER(created_by) TABLESPACE pg_default;
CREATE INDEX rpr_lst_mod_by_fk  ON RPS_RETAINER(last_modified_by) TABLESPACE pg_default;

CREATE INDEX rah_cas_fk         ON CASE_RETENTION_AUDIT_HERITAGE(cas_id) TABLESPACE pg_default;
CREATE INDEX rah_rpt_fk         ON CASE_RETENTION_AUDIT_HERITAGE(rpt_id) TABLESPACE pg_default;
CREATE INDEX rah_c_username_fk  ON CASE_RETENTION_AUDIT_HERITAGE(c_username) TABLESPACE pg_default;
CREATE INDEX rah_r_creator_fk   ON CASE_RETENTION_AUDIT_HERITAGE(r_creator_name) TABLESPACE pg_default;
CREATE INDEX rah_r_modifier_fk  ON CASE_RETENTION_AUDIT_HERITAGE(r_modifier) TABLESPACE pg_default;
CREATE INDEX rah_owner_fk       ON CASE_RETENTION_AUDIT_HERITAGE(owner_name) TABLESPACE pg_default;

CREATE INDEX rcc_cre_by_fk      ON RETENTION_CONFIDENCE_CATEGORY_MAPPER(created_by) TABLESPACE pg_default;
CREATE INDEX rcc_lst_mod_by_fk  ON RETENTION_CONFIDENCE_CATEGORY_MAPPER(last_modified_by) TABLESPACE pg_default;

CREATE INDEX cao_rpt_fk         ON CASE_OVERFLOW(rpt_id) TABLESPACE pg_default;

--v2 
CREATE INDEX cas_cn_idx         ON COURT_CASE(case_number)                  TABLESPACE pg_default;
CREATE INDEX cth_cn_idx         ON COURTHOUSE(UPPER(courthouse_name))       TABLESPACE pg_default;  
CREATE INDEX ctr_cn_idx         ON COURTROOM(UPPER(courtroom_name))         TABLESPACE pg_default;
CREATE INDEX dfc_dn_idx         ON DEFENCE(UPPER(defence_name))             TABLESPACE pg_default;
CREATE INDEX dfd_dn_idx         ON DEFENDANT(UPPER(defendant_name))         TABLESPACE pg_default;
CREATE INDEX eve_ei_ic_idx      ON EVENT(event_id,is_current)               TABLESPACE pg_default;
CREATE INDEX eve_ts_idx         ON EVENT(event_ts)                          TABLESPACE pg_default;
CREATE INDEX hea_hd_idx         ON HEARING(hearing_date)                    TABLESPACE pg_default;
CREATE INDEX jud_jn_idx         ON JUDGE(UPPER(judge_name))                 TABLESPACE pg_default;
CREATE INDEX med_mf_idx         ON MEDIA(media_file)                        TABLESPACE pg_default;
CREATE INDEX med_st_et_idx      ON MEDIA(start_ts,end_ts)                   TABLESPACE pg_default;
CREATE INDEX prn_pn_idx         ON PROSECUTOR(UPPER(prosecutor_name))       TABLESPACE pg_default;
CREATE INDEX usr_un_idx         ON USER_ACCOUNT(user_full_name)             TABLESPACE pg_default;
CREATE INDEX usr_upea_idx       ON USER_ACCOUNT(UPPER(user_email_address))  TABLESPACE pg_default;
CREATE INDEX usr_ag_idx         ON USER_ACCOUNT(account_guid)               TABLESPACE pg_default;

--v6
CREATE INDEX event_event_id_is_current_idx
    ON darts.event USING btree
    (event_id ASC NULLS LAST, is_current ASC NULLS LAST)
    TABLESPACE pg_default;

CREATE UNIQUE INDEX event_handler_event_type_event_event_sub_type_unq
    ON darts.event_handler USING btree
    (event_type COLLATE pg_catalog."default" ASC NULLS LAST, event_sub_type COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default
    WHERE active;

CREATE UNIQUE INDEX retention_policy_type_type_unq
    ON darts.retention_policy_type USING btree
    (fixed_policy_key COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default
    WHERE policy_end_ts IS NULL;
	
CREATE INDEX eod_manifest_file_idx
    ON darts.external_object_directory USING btree
    (manifest_file COLLATE pg_catalog."default" text_pattern_ops ASC NULLS LAST)
    TABLESPACE pg_default;
	
CREATE INDEX dfd_dn_trgm_idx ON defendant USING gin (defendant_name gin_trgm_ops);

CREATE INDEX eve_evt_trgm_idx ON event USING gin (event_text gin_trgm_ops);

CREATE INDEX jud_jn_trgm_idx ON judge USING gin (judge_name gin_trgm_ops);

CREATE INDEX cas_cn_trgm_idx ON court_case USING gin (case_number gin_trgm_ops);

CREATE INDEX ctr_cn_trgm_idx ON courtroom USING gin (courtroom_name gin_trgm_ops);

-- v12
CREATE INDEX med_chronicle_id_idx ON media (chronicle_id);
