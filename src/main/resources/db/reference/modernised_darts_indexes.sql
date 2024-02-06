--modernised_darts_indexes
--
-- v1 aligned to v60 of the modernised schema, v19 security, v6 retention
-- commented statements, unnecessary due to presence of PK, which will support the FK in question.
-- v2 additional indexes to accommodate initial access to driving site table
-- adding storage clauses to all indexes
-- v3 remove indexes on hea_id and cas_id from transcription


SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

-- v60 application
CREATE INDEX ann_cur_own_fk ON ANNOTATION (current_owner) TABLESPACE darts_indexes;
CREATE INDEX ann_cre_by_fk ON ANNOTATION (created_by) TABLESPACE darts_indexes;
CREATE INDEX ann_lst_mod_by_fk ON ANNOTATION (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX ado_ann_fk ON ANNOTATION_DOCUMENT (ann_id) TABLESPACE darts_indexes;

CREATE INDEX aud_cas_fk ON AUDIT (cas_id) TABLESPACE darts_indexes;
CREATE INDEX aud_aua_fk ON AUDIT (aua_id) TABLESPACE darts_indexes;
CREATE INDEX aud_usr_fk ON AUDIT (usr_id) TABLESPACE darts_indexes;
CREATE INDEX aud_cre_by_fk ON AUDIT (created_by) TABLESPACE darts_indexes;
CREATE INDEX aud_lst_mod_by_fk ON AUDIT (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX aua_cre_by_fk ON AUDIT_ACTIVITY (created_by) TABLESPACE darts_indexes;
CREATE INDEX aua_lst_mod_by_fk ON AUDIT_ACTIVITY (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX aut_cre_by_fk ON AUTOMATED_TASK (created_by) TABLESPACE darts_indexes;
CREATE INDEX aut_lst_mod_by_fk ON AUTOMATED_TASK (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX cad_cas_fk ON CASE_DOCUMENT (cas_id) TABLESPACE darts_indexes;

--CREATE INDEX caj_cas_fk         ON CASE_JUDGE_AE(cas_id);
CREATE INDEX caj_jud_fk ON CASE_JUDGE_AE (jud_id) TABLESPACE darts_indexes;

CREATE INDEX cas_evh_fk ON COURT_CASE (evh_id) TABLESPACE darts_indexes;
CREATE INDEX cas_cth_fk ON COURT_CASE (cth_id) TABLESPACE darts_indexes;
CREATE INDEX cas_cre_by_fk ON COURT_CASE (created_by) TABLESPACE darts_indexes;
CREATE INDEX cas_lst_mod_by_fk ON COURT_CASE (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX cth_cre_by_fk ON COURTHOUSE (created_by) TABLESPACE darts_indexes;
CREATE INDEX cth_lst_mod_by_fk ON COURTHOUSE (last_modified_by) TABLESPACE darts_indexes;

--CREATE INDEX cra_cth_fk         ON COURTHOUSE_REGION_AE(cth_id);
CREATE INDEX cra_reg_fk ON COURTHOUSE_REGION_AE (reg_id) TABLESPACE darts_indexes;

CREATE INDEX ctr_cth_fk ON COURTROOM (cth_id) TABLESPACE darts_indexes;
CREATE INDEX ctr_cre_by_fk ON COURTROOM (created_by) TABLESPACE darts_indexes;

CREATE INDEX dal_cre_by_fk ON DAILY_LIST (created_by) TABLESPACE darts_indexes;
CREATE INDEX dal_lst_mod_by ON DAILY_LIST (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX dfc_cas_fk ON DEFENCE (cas_id) TABLESPACE darts_indexes;
CREATE INDEX dfc_cre_by_fk ON DEFENCE (created_by) TABLESPACE darts_indexes;
CREATE INDEX dfc_lst_mod_by_fk ON DEFENCE (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX dfd_cas_fk ON DEFENDANT (cas_id) TABLESPACE darts_indexes;
CREATE INDEX dfd_cre_by_fk ON DEFENDANT (created_by) TABLESPACE darts_indexes;
CREATE INDEX dfd_lst_mod_by_fk ON DEFENDANT (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX eve_ctr_fk ON EVENT (ctr_id) TABLESPACE darts_indexes;
CREATE INDEX eve_evh_fk ON EVENT (evh_id) TABLESPACE darts_indexes;
CREATE INDEX eve_cre_by_fk ON EVENT (created_by) TABLESPACE darts_indexes;
CREATE INDEX eve_lst_mod_by_fk ON EVENT (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX evh_cre_by_fk ON EVENT_HANDLER (created_by) TABLESPACE darts_indexes;

CREATE INDEX eod_med_fk ON EXTERNAL_OBJECT_DIRECTORY (med_id) TABLESPACE darts_indexes;
CREATE INDEX eod_trd_fk ON EXTERNAL_OBJECT_DIRECTORY (trd_id) TABLESPACE darts_indexes;
CREATE INDEX eod_cad_fk ON EXTERNAL_OBJECT_DIRECTORY (cad_id) TABLESPACE darts_indexes;
CREATE INDEX eod_ado_fk ON EXTERNAL_OBJECT_DIRECTORY (ado_id) TABLESPACE darts_indexes;
CREATE INDEX eod_cre_by_fk ON EXTERNAL_OBJECT_DIRECTORY (created_by) TABLESPACE darts_indexes;
CREATE INDEX eod_lst_mod_by_fk ON EXTERNAL_OBJECT_DIRECTORY (last_modified_by) TABLESPACE darts_indexes;
CREATE INDEX eod_ors_fk ON EXTERNAL_OBJECT_DIRECTORY (ors_id) TABLESPACE darts_indexes;
CREATE INDEX eod_elt_fk ON EXTERNAL_OBJECT_DIRECTORY (elt_id) TABLESPACE darts_indexes;

CREATE INDEX esa_cre_by_fk ON EXTERNAL_SERVICE_AUTH_TOKEN (created_by) TABLESPACE darts_indexes;
CREATE INDEX esa_lst_mod_by_fk ON EXTERNAL_SERVICE_AUTH_TOKEN (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX hea_cas_fk ON HEARING (cas_id) TABLESPACE darts_indexes;
CREATE INDEX hea_ctr_fk ON HEARING (ctr_id) TABLESPACE darts_indexes;
CREATE INDEX hea_cre_by_fk ON HEARING (created_by) TABLESPACE darts_indexes;
CREATE INDEX hea_lst_mod_by_fk ON HEARING (last_modified_by) TABLESPACE darts_indexes;

--CREATE INDEX haa_hea_fk         ON HEARING_ANNOTATION_AE(hea_id);
CREATE INDEX haa_ann_fk ON HEARING_ANNOTATION_AE (ann_id) TABLESPACE darts_indexes;

--CREATE INDEX hee_hea_fk         ON HEARING_EVENT_AE(hea_id);
CREATE INDEX hee_eve_fk ON HEARING_EVENT_AE (eve_id) TABLESPACE darts_indexes;

--CREATE INDEX hej_hea_fk         ON HEARING_JUDGE_AE(hea_id);
CREATE INDEX hej_jud_fk ON HEARING_JUDGE_AE (jud_id) TABLESPACE darts_indexes;

--CREATE INDEX hem_hea_fk         ON HEARING_MEDIA_AE(hea_id);
CREATE INDEX hem_med_fk ON HEARING_MEDIA_AE (med_id) TABLESPACE darts_indexes;

CREATE INDEX jud_cre_by_fk ON JUDGE (created_by) TABLESPACE darts_indexes;
CREATE INDEX jud_lst_mod_by_fk ON JUDGE (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX med_ctr_id ON MEDIA (ctr_id) TABLESPACE darts_indexes;
CREATE INDEX med_cre_by_fk ON MEDIA (created_by) TABLESPACE darts_indexes;
CREATE INDEX med_lst_mod_by_fk ON MEDIA (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX mer_hea_fk ON MEDIA_REQUEST (hea_id) TABLESPACE darts_indexes;
CREATE INDEX mer_cre_by_fk ON MEDIA_REQUEST (created_by) TABLESPACE darts_indexes;
CREATE INDEX mer_lst_mod_by_fk ON MEDIA_REQUEST (last_modified_by) TABLESPACE darts_indexes;
CREATE INDEX mer_req_fk ON MEDIA_REQUEST (requestor) TABLESPACE darts_indexes;
CREATE INDEX mer_cur_own_fk ON MEDIA_REQUEST (current_owner) TABLESPACE darts_indexes;

CREATE INDEX nod_ctr_fk ON NODE_REGISTER (ctr_id) TABLESPACE darts_indexes;
CREATE INDEX nod_cre_by_fk ON NODE_REGISTER (created_by) TABLESPACE darts_indexes;

CREATE INDEX not_cas_fk ON NOTIFICATION (cas_id) TABLESPACE darts_indexes;
CREATE INDEX not_cre_by_fk ON NOTIFICATION (created_by) TABLESPACE darts_indexes;
CREATE INDEX not_lst_mod_fk ON NOTIFICATION (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX prn_cas_fk ON PROSECUTOR (cas_id) TABLESPACE darts_indexes;
CREATE INDEX prn_cre_by_fk ON PROSECUTOR (created_by) TABLESPACE darts_indexes;
CREATE INDEX prn_lst_mod_by_fk ON PROSECUTOR (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX rep_cre_by_fk ON REPORT (created_by) TABLESPACE darts_indexes;
CREATE INDEX rep_lst_mod_by_fk ON REPORT (last_modified_by) TABLESPACE darts_indexes;

--CREATE INDEX tra_cas_fk         ON TRANSCRIPTION(cas_id) TABLESPACE darts_indexes;
CREATE INDEX tra_ctr_fk ON TRANSCRIPTION (ctr_id) TABLESPACE darts_indexes;
CREATE INDEX tra_trs_fk ON TRANSCRIPTION (trs_id) TABLESPACE darts_indexes;
CREATE INDEX tra_tru_fk ON TRANSCRIPTION (tru_id) TABLESPACE darts_indexes;
--CREATE INDEX tra_hea_fk         ON TRANSCRIPTION(hea_id) TABLESPACE darts_indexes;
CREATE INDEX tra_cre_by_fk ON TRANSCRIPTION (created_by) TABLESPACE darts_indexes;
CREATE INDEX tra_lst_mod_by_fk ON TRANSCRIPTION (last_modified_by) TABLESPACE darts_indexes;
CREATE INDEX tra_trt_fk ON TRANSCRIPTION (trt_id) TABLESPACE darts_indexes;

CREATE INDEX trc_tra_fk ON TRANSCRIPTION_COMMENT (tra_id) TABLESPACE darts_indexes;
CREATE INDEX trc_trw_fk ON TRANSCRIPTION_COMMENT (trw_id) TABLESPACE darts_indexes;
CREATE INDEX trc_auth_fk ON TRANSCRIPTION_COMMENT (author) TABLESPACE darts_indexes;
CREATE INDEX trc_cre_by_fk ON TRANSCRIPTION_COMMENT (created_by) TABLESPACE darts_indexes;
CREATE INDEX trc_lst_mod_by_fk ON TRANSCRIPTION_COMMENT (last_modified_by) TABLESPACE darts_indexes;

CREATE INDEX trd_tra_fk ON TRANSCRIPTION_DOCUMENT (tra_id) TABLESPACE darts_indexes;

CREATE INDEX trw_tra_fk ON TRANSCRIPTION_WORKFLOW (tra_id) TABLESPACE darts_indexes;
CREATE INDEX trw_trs_fk ON TRANSCRIPTION_WORKFLOW (trs_id) TABLESPACE darts_indexes;
CREATE INDEX trw_wrkflw_act_fk ON TRANSCRIPTION_WORKFLOW (workflow_actor) TABLESPACE darts_indexes;

CREATE INDEX trm_mer_fk ON TRANSFORMED_MEDIA (mer_id) TABLESPACE darts_indexes;

CREATE INDEX tod_cre_by_fk ON TRANSIENT_OBJECT_DIRECTORY (created_by) TABLESPACE darts_indexes;
CREATE INDEX tod_lst_mod_by_fk ON TRANSIENT_OBJECT_DIRECTORY (last_modified_by) TABLESPACE darts_indexes;
CREATE INDEX tod_trm_fk ON TRANSIENT_OBJECT_DIRECTORY (trm_id) TABLESPACE darts_indexes;
CREATE INDEX tod_ors_fk ON TRANSIENT_OBJECT_DIRECTORY (ors_id) TABLESPACE darts_indexes;

--v19 security
CREATE INDEX grp_rol_fk ON SECURITY_GROUP (rol_id) TABLESPACE darts_indexes;
CREATE INDEX grp_cre_by_fk ON SECURITY_GROUP (created_by) TABLESPACE darts_indexes;
CREATE INDEX grp_lst_mod_by_fk ON SECURITY_GROUP (last_modified_by) TABLESPACE darts_indexes;

--CREATE INDEX grc_grp_fk         ON SECURITY_GROUP_COURTHOUSE_AE(grp_id);
CREATE INDEX grc_cth_fk ON SECURITY_GROUP_COURTHOUSE_AE (cth_id) TABLESPACE darts_indexes;

CREATE INDEX gua_grp_fk ON SECURITY_GROUP_USER_ACCOUNT_AE (grp_id) TABLESPACE darts_indexes;
--CREATE INDEX gua_usr_fk         ON SECURITY_GROUP_USER_ACCOUNT_AE(usr_id);

--CREATE INDEX rop_rol_fk         ON SECURITY_ROLE_PERMISSION_AE(rol_id);
CREATE INDEX rop_per_fk ON SECURITY_ROLE_PERMISSION_AE (per_id) TABLESPACE darts_indexes;

--v6 retention
CREATE INDEX car_cas_fk ON CASE_RETENTION (cas_id) TABLESPACE darts_indexes;
CREATE INDEX car_rpt_fk ON CASE_RETENTION (rpt_id) TABLESPACE darts_indexes;
CREATE INDEX car_cre_by_fk ON CASE_RETENTION (created_by) TABLESPACE darts_indexes;
CREATE INDEX car_lst_mod_by_fk ON CASE_RETENTION (last_modified_by) TABLESPACE darts_indexes;
CREATE INDEX car_sub_by_fk ON CASE_RETENTION (submitted_by) TABLESPACE darts_indexes;
CREATE INDEX car_cmr_fk ON CASE_RETENTION (cmr_id) TABLESPACE darts_indexes;

CREATE INDEX cmr_cas_fk ON CASE_MANAGEMENT_RETENTION (cas_id) TABLESPACE darts_indexes;
CREATE INDEX cmr_rpt_fk ON CASE_MANAGEMENT_RETENTION (rpt_id) TABLESPACE darts_indexes;
CREATE INDEX cmr_eve_fk ON CASE_MANAGEMENT_RETENTION (eve_id) TABLESPACE darts_indexes;

CREATE INDEX rpt_cre_by_fk ON RETENTION_POLICY_TYPE (created_by) TABLESPACE darts_indexes;
CREATE INDEX rpt_lst_mod_by_fk ON RETENTION_POLICY_TYPE (last_modified_by) TABLESPACE darts_indexes;


--v2 
CREATE INDEX cas_cn_idx ON COURT_CASE (case_number) TABLESPACE darts_indexes;
CREATE INDEX cth_cn_idx ON COURTHOUSE (courthouse_name) TABLESPACE darts_indexes;
CREATE INDEX ctr_cn_idx ON COURTROOM (courtroom_name) TABLESPACE darts_indexes;
CREATE INDEX dfc_dn_idx ON DEFENCE (defence_name) TABLESPACE darts_indexes;
CREATE INDEX dfd_dn_idx ON DEFENDANT (defendant_name) TABLESPACE darts_indexes;
CREATE INDEX hea_hd_idx ON HEARING (hearing_date) TABLESPACE darts_indexes;
CREATE INDEX jud_jn_idx ON JUDGE (judge_name) TABLESPACE darts_indexes;
CREATE INDEX prn_pn_idx ON PROSECUTOR (prosecutor_name) TABLESPACE darts_indexes;
CREATE INDEX usr_un_idx ON USER_ACCOUNT (user_name) TABLESPACE darts_indexes;

