CREATE INDEX ann_cur_own_fk     ON ANNOTATION(current_owner);
CREATE INDEX ann_cre_by_fk      ON ANNOTATION(created_by);
CREATE INDEX ann_lst_mod_by_fk  ON ANNOTATION(last_modified_by);

CREATE INDEX ado_ann_fk         ON ANNOTATION_DOCUMENT(ann_id);

CREATE INDEX aud_cas_fk         ON AUDIT(cas_id);
CREATE INDEX aud_aua_fk         ON AUDIT(aua_id);
CREATE INDEX aud_usr_fk         ON AUDIT(usr_id);
CREATE INDEX aud_cre_by_fk      ON AUDIT(created_by);
CREATE INDEX aud_lst_mod_by_fk  ON AUDIT(last_modified_by);

CREATE INDEX aua_cre_by_fk      ON AUDIT_ACTIVITY(created_by);
CREATE INDEX aua_lst_mod_by_fk  ON AUDIT_ACTIVITY(last_modified_by);

CREATE INDEX aut_cre_by_fk      ON AUTOMATED_TASK(created_by);
CREATE INDEX aut_lst_mod_by_fk  ON AUTOMATED_TASK(last_modified_by);

CREATE INDEX cad_cas_fk         ON CASE_DOCUMENT(cas_id);

CREATE INDEX caj_jud_fk         ON CASE_JUDGE_AE(jud_id);

CREATE INDEX cas_evh_fk         ON COURT_CASE(evh_id);
CREATE INDEX cas_cth_fk         ON COURT_CASE(cth_id);
CREATE INDEX cas_cre_by_fk      ON COURT_CASE(created_by);
CREATE INDEX cas_lst_mod_by_fk  ON COURT_CASE(last_modified_by);

CREATE INDEX cth_cre_by_fk      ON COURTHOUSE(created_by);
CREATE INDEX cth_lst_mod_by_fk  ON COURTHOUSE(last_modified_by);

CREATE INDEX cra_reg_fk         ON COURTHOUSE_REGION_AE(reg_id);

CREATE INDEX ctr_cth_fk         ON COURTROOM(cth_id);
CREATE INDEX ctr_cre_by_fk      ON COURTROOM(created_by);

CREATE INDEX dal_cre_by_fk      ON DAILY_LIST(created_by);
CREATE INDEX dal_lst_mod_by     ON DAILY_LIST(last_modified_by);

CREATE INDEX dfc_cas_fk         ON DEFENCE(cas_id);
CREATE INDEX dfc_cre_by_fk      ON DEFENCE(created_by);
CREATE INDEX dfc_lst_mod_by_fk  ON DEFENCE(last_modified_by);

CREATE INDEX dfd_cas_fk         ON DEFENDANT(cas_id);
CREATE INDEX dfd_cre_by_fk      ON DEFENDANT(created_by);
CREATE INDEX dfd_lst_mod_by_fk  ON DEFENDANT(last_modified_by);

CREATE INDEX eve_ctr_fk         ON EVENT(ctr_id);
CREATE INDEX eve_evh_fk         ON EVENT(evh_id);
CREATE INDEX eve_cre_by_fk      ON EVENT(created_by);
CREATE INDEX eve_lst_mod_by_fk  ON EVENT(last_modified_by);

CREATE INDEX evh_cre_by_fk      ON EVENT_HANDLER(created_by);

CREATE INDEX eod_med_fk         ON EXTERNAL_OBJECT_DIRECTORY(med_id);
CREATE INDEX eod_trd_fk         ON EXTERNAL_OBJECT_DIRECTORY(trd_id);
CREATE INDEX eod_cad_fk         ON EXTERNAL_OBJECT_DIRECTORY(cad_id);
CREATE INDEX eod_ado_fk         ON EXTERNAL_OBJECT_DIRECTORY(ado_id);
CREATE INDEX eod_cre_by_fk      ON EXTERNAL_OBJECT_DIRECTORY(created_by);
CREATE INDEX eod_lst_mod_by_fk  ON EXTERNAL_OBJECT_DIRECTORY(last_modified_by);
CREATE INDEX eod_ors_fk         ON EXTERNAL_OBJECT_DIRECTORY(ors_id);
CREATE INDEX eod_elt_fk         ON EXTERNAL_OBJECT_DIRECTORY(elt_id);

CREATE INDEX esa_cre_by_fk      ON EXTERNAL_SERVICE_AUTH_TOKEN(created_by);
CREATE INDEX esa_lst_mod_by_fk  ON EXTERNAL_SERVICE_AUTH_TOKEN(last_modified_by);

CREATE INDEX hea_cas_fk         ON HEARING(cas_id);
CREATE INDEX hea_ctr_fk         ON HEARING(ctr_id);
CREATE INDEX hea_cre_by_fk      ON HEARING(created_by);
CREATE INDEX hea_lst_mod_by_fk  ON HEARING(last_modified_by);

CREATE INDEX haa_ann_fk         ON HEARING_ANNOTATION_AE(ann_id);

CREATE INDEX hee_eve_fk         ON HEARING_EVENT_AE(eve_id);

CREATE INDEX hej_jud_fk         ON HEARING_JUDGE_AE(jud_id);

CREATE INDEX hem_med_fk         ON HEARING_MEDIA_AE(med_id);

CREATE INDEX jud_cre_by_fk      ON JUDGE(created_by);
CREATE INDEX jud_lst_mod_by_fk  ON JUDGE(last_modified_by);

CREATE INDEX med_ctr_id         ON MEDIA(ctr_id);
CREATE INDEX med_cre_by_fk      ON MEDIA(created_by);
CREATE INDEX med_lst_mod_by_fk  ON MEDIA(last_modified_by);

CREATE INDEX mer_hea_fk         ON MEDIA_REQUEST(hea_id);
CREATE INDEX mer_cre_by_fk      ON MEDIA_REQUEST(created_by);
CREATE INDEX mer_lst_mod_by_fk  ON MEDIA_REQUEST(last_modified_by);
CREATE INDEX mer_req_fk         ON MEDIA_REQUEST(requestor);
CREATE INDEX mer_cur_own_fk     ON MEDIA_REQUEST(current_owner);

CREATE INDEX nod_ctr_fk         ON NODE_REGISTER(ctr_id);
CREATE INDEX nod_cre_by_fk      ON NODE_REGISTER(created_by);

CREATE INDEX not_cas_fk         ON NOTIFICATION(cas_id);
CREATE INDEX not_cre_by_fk      ON NOTIFICATION(created_by);
CREATE INDEX not_lst_mod_fk     ON NOTIFICATION(last_modified_by);

CREATE INDEX prn_cas_fk         ON PROSECUTOR(cas_id);
CREATE INDEX prn_cre_by_fk      ON PROSECUTOR(created_by);
CREATE INDEX prn_lst_mod_by_fk  ON PROSECUTOR(last_modified_by);

CREATE INDEX rep_cre_by_fk      ON REPORT(created_by);
CREATE INDEX rep_lst_mod_by_fk  ON REPORT(last_modified_by);

CREATE INDEX tra_ctr_fk         ON TRANSCRIPTION(ctr_id);
CREATE INDEX tra_trs_fk         ON TRANSCRIPTION(trs_id);
CREATE INDEX tra_tru_fk         ON TRANSCRIPTION(tru_id);
CREATE INDEX tra_cre_by_fk      ON TRANSCRIPTION(created_by);
CREATE INDEX tra_lst_mod_by_fk  ON TRANSCRIPTION(last_modified_by);
CREATE INDEX tra_trt_fk         ON TRANSCRIPTION(trt_id);

CREATE INDEX trc_tra_fk         ON TRANSCRIPTION_COMMENT(tra_id);
CREATE INDEX trc_trw_fk         ON TRANSCRIPTION_COMMENT(trw_id);
CREATE INDEX trc_auth_fk        ON TRANSCRIPTION_COMMENT(author);
CREATE INDEX trc_cre_by_fk      ON TRANSCRIPTION_COMMENT(created_by);
CREATE INDEX trc_lst_mod_by_fk  ON TRANSCRIPTION_COMMENT(last_modified_by);

CREATE INDEX trd_tra_fk         ON TRANSCRIPTION_DOCUMENT(tra_id);

CREATE INDEX trw_tra_fk         ON TRANSCRIPTION_WORKFLOW(tra_id);
CREATE INDEX trw_trs_fk         ON TRANSCRIPTION_WORKFLOW(trs_id);
CREATE INDEX trw_wrkflw_act_fk  ON TRANSCRIPTION_WORKFLOW(workflow_actor);

CREATE INDEX trm_mer_fk         ON TRANSFORMED_MEDIA(mer_id);

CREATE INDEX tod_cre_by_fk      ON TRANSIENT_OBJECT_DIRECTORY(created_by);
CREATE INDEX tod_lst_mod_by_fk  ON TRANSIENT_OBJECT_DIRECTORY(last_modified_by);
CREATE INDEX tod_trm_fk         ON TRANSIENT_OBJECT_DIRECTORY(trm_id);
CREATE INDEX tod_ors_fk         ON TRANSIENT_OBJECT_DIRECTORY(ors_id);

--v19 security
CREATE INDEX grp_rol_fk         ON SECURITY_GROUP(rol_id);
CREATE INDEX grp_cre_by_fk      ON SECURITY_GROUP(created_by);
CREATE INDEX grp_lst_mod_by_fk  ON SECURITY_GROUP(last_modified_by);

CREATE INDEX grc_cth_fk         ON SECURITY_GROUP_COURTHOUSE_AE(cth_id);

CREATE INDEX gua_grp_fk         ON SECURITY_GROUP_USER_ACCOUNT_AE(grp_id);

CREATE INDEX rop_per_fk         ON SECURITY_ROLE_PERMISSION_AE(per_id);

--v6 retention
CREATE INDEX car_cas_fk         ON CASE_RETENTION(cas_id);
CREATE INDEX car_rpt_fk         ON CASE_RETENTION(rpt_id);
CREATE INDEX car_cre_by_fk      ON CASE_RETENTION(created_by);
CREATE INDEX car_lst_mod_by_fk  ON CASE_RETENTION(last_modified_by);
CREATE INDEX car_sub_by_fk      ON CASE_RETENTION(submitted_by);
CREATE INDEX car_cmr_fk         ON CASE_RETENTION(cmr_id);

CREATE INDEX cmr_cas_fk         ON CASE_MANAGEMENT_RETENTION(cas_id);
CREATE INDEX cmr_rpt_fk         ON CASE_MANAGEMENT_RETENTION(rpt_id);
CREATE INDEX cmr_eve_fk         ON CASE_MANAGEMENT_RETENTION(eve_id);

CREATE INDEX rpt_cre_by_fk      ON RETENTION_POLICY_TYPE(created_by);
CREATE INDEX rpt_lst_mod_by_fk  ON RETENTION_POLICY_TYPE(last_modified_by);


--v2 
CREATE INDEX cas_cn_idx         ON COURT_CASE(case_number);
CREATE INDEX cth_cn_idx         ON COURTHOUSE(courthouse_name);  
CREATE INDEX ctr_cn_idx         ON COURTROOM(courtroom_name);
CREATE INDEX dfc_dn_idx         ON DEFENCE(defence_name);
CREATE INDEX dfd_dn_idx         ON DEFENDANT(defendant_name);
CREATE INDEX hea_hd_idx         ON HEARING(hearing_date);
CREATE INDEX jud_jn_idx         ON JUDGE(judge_name);
CREATE INDEX prn_pn_idx         ON PROSECUTOR(prosecutor_name);
CREATE INDEX usr_un_idx         ON USER_ACCOUNT(user_name);

