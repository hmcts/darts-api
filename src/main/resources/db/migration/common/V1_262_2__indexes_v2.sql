--modernised_darts_indexes
--
-- v1 aligned to v60 of the modernised schema, v19 security, v6 retention
-- commented statements, unnecessary due to presence of PK, which will support the FK in question.
-- v2 additional indexes to accommodate initial access to driving site table
-- adding storage clauses to all indexes
-- v3 remove indexes on hea_id and cas_id from transcription


-- v60 application
CREATE INDEX ann_cur_own_fk     ON annotation(current_owner);
CREATE INDEX ann_cre_by_fk      ON annotation(created_by);
CREATE INDEX ann_lst_mod_by_fk  ON annotation(last_modified_by);

CREATE INDEX ado_ann_fk         ON annotation_document(ann_id);

CREATE INDEX aud_cas_fk         ON audit(cas_id);
CREATE INDEX aud_aua_fk         ON audit(aua_id);
CREATE INDEX aud_usr_fk         ON audit(usr_id);
CREATE INDEX aud_cre_by_fk      ON audit(created_by);
CREATE INDEX aud_lst_mod_by_fk  ON audit(last_modified_by);

CREATE INDEX aua_cre_by_fk      ON audit_activity(created_by);
CREATE INDEX aua_lst_mod_by_fk  ON audit_activity(last_modified_by);

CREATE INDEX aut_cre_by_fk      ON automated_task(created_by);
CREATE INDEX aut_lst_mod_by_fk  ON automated_task(last_modified_by);

CREATE INDEX cad_cas_fk         ON case_document(cas_id);

--CREATE INDEX caj_cas_fk         On case_judge_ae(cas_id);
CREATE INDEX caj_jud_fk         ON case_judge_ae(jud_id);

CREATE INDEX cas_evh_fk         ON court_case(evh_id);
CREATE INDEX cas_cth_fk         ON court_case(cth_id);
CREATE INDEX cas_cre_by_fk      ON court_case(created_by);
CREATE INDEX cas_lst_mod_by_fk  ON court_case(last_modified_by);

CREATE INDEX cth_cre_by_fk      ON courthouse(created_by);
CREATE INDEX cth_lst_mod_by_fk  ON courthouse(last_modified_by);

--CREATE INDEX cra_cth_fk         On courthouse_region_ae(cth_id);
CREATE INDEX cra_reg_fk         ON courthouse_region_ae(reg_id);

CREATE INDEX ctr_cth_fk         ON courtroom(cth_id);
CREATE INDEX ctr_cre_by_fk      ON courtroom(created_by);

CREATE INDEX dal_cre_by_fk      ON daily_list(created_by);
CREATE INDEX dal_lst_mod_by     ON daily_list(last_modified_by);

CREATE INDEX dfc_cas_fk         ON defence(cas_id);
CREATE INDEX dfc_cre_by_fk      ON defence(created_by);
CREATE INDEX dfc_lst_mod_by_fk  ON defence(last_modified_by);

CREATE INDEX dfd_cas_fk         ON defendant(cas_id);
CREATE INDEX dfd_cre_by_fk      ON defendant(created_by);
CREATE INDEX dfd_lst_mod_by_fk  ON defendant(last_modified_by);

CREATE INDEX eve_ctr_fk         ON event(ctr_id);
CREATE INDEX eve_evh_fk         ON event(evh_id);
CREATE INDEX eve_cre_by_fk      ON event(created_by);
CREATE INDEX eve_lst_mod_by_fk  ON event(last_modified_by);

CREATE INDEX evh_cre_by_fk      ON event_handler(created_by);

CREATE INDEX eod_med_fk         ON external_object_directory(med_id);
CREATE INDEX eod_trd_fk         ON external_object_directory(trd_id);
CREATE INDEX eod_cad_fk         ON external_object_directory(cad_id);
CREATE INDEX eod_ado_fk         ON external_object_directory(ado_id);
CREATE INDEX eod_cre_by_fk      ON external_object_directory(created_by);
CREATE INDEX eod_lst_mod_by_fk  ON external_object_directory(last_modified_by);
CREATE INDEX eod_ors_fk         ON external_object_directory(ors_id);
CREATE INDEX eod_elt_fk         ON external_object_directory(elt_id);

CREATE INDEX esa_cre_by_fk      ON external_service_auth_token(created_by);
CREATE INDEX esa_lst_mod_by_fk  ON external_service_auth_token(last_modified_by);

CREATE INDEX hea_cas_fk         ON hearing(cas_id);
CREATE INDEX hea_ctr_fk         ON hearing(ctr_id);
CREATE INDEX hea_cre_by_fk      ON hearing(created_by);
CREATE INDEX hea_lst_mod_by_fk  ON hearing(last_modified_by);

--CREATE INDEX haa_hea_fk         On hearing_annotation_ae(hea_id);
CREATE INDEX haa_ann_fk         ON hearing_annotation_ae(ann_id);

--CREATE INDEX hee_hea_fk         On hearing_event_ae(hea_id);
CREATE INDEX hee_eve_fk         ON hearing_event_ae(eve_id);

--CREATE INDEX hej_hea_fk         On hearing_judge_ae(hea_id);
CREATE INDEX hej_jud_fk         ON hearing_judge_ae(jud_id);

--CREATE INDEX hem_hea_fk         On hearing_media_ae(hea_id);
CREATE INDEX hem_med_fk         ON hearing_media_ae(med_id);

CREATE INDEX jud_cre_by_fk      ON judge(created_by);
CREATE INDEX jud_lst_mod_by_fk  ON judge(last_modified_by);

CREATE INDEX med_ctr_id         ON media(ctr_id);
CREATE INDEX med_cre_by_fk      ON media(created_by);
CREATE INDEX med_lst_mod_by_fk  ON media(last_modified_by);

CREATE INDEX mer_hea_fk         ON media_request(hea_id);
CREATE INDEX mer_cre_by_fk      ON media_request(created_by);
CREATE INDEX mer_lst_mod_by_fk  ON media_request(last_modified_by);
CREATE INDEX mer_req_fk         ON media_request(requestor);
CREATE INDEX mer_cur_own_fk     ON media_request(current_owner);

CREATE INDEX nod_ctr_fk         ON node_register(ctr_id);
CREATE INDEX nod_cre_by_fk      ON node_register(created_by);

CREATE INDEX not_cas_fk         ON notification(cas_id);
CREATE INDEX not_cre_by_fk      ON notification(created_by);
CREATE INDEX not_lst_mod_fk     ON notification(last_modified_by);

CREATE INDEX prn_cas_fk         ON prosecutor(cas_id);
CREATE INDEX prn_cre_by_fk      ON prosecutor(created_by);
CREATE INDEX prn_lst_mod_by_fk  ON prosecutor(last_modified_by);

CREATE INDEX rep_cre_by_fk      ON report(created_by);
CREATE INDEX rep_lst_mod_by_fk  ON report(last_modified_by);

--CREATE INDEX tra_cas_fk         On transcription(cas_id);
CREATE INDEX tra_ctr_fk         ON transcription(ctr_id);
CREATE INDEX tra_trs_fk         ON transcription(trs_id);
CREATE INDEX tra_tru_fk         ON transcription(tru_id);
--CREATE INDEX tra_hea_fk         On transcription(hea_id);
CREATE INDEX tra_cre_by_fk      ON transcription(created_by);
CREATE INDEX tra_lst_mod_by_fk  ON transcription(last_modified_by);
CREATE INDEX tra_trt_fk         ON transcription(trt_id);

CREATE INDEX trc_tra_fk         ON transcription_comment(tra_id);
CREATE INDEX trc_trw_fk         ON transcription_comment(trw_id);
CREATE INDEX trc_auth_fk        ON transcription_comment(author);
CREATE INDEX trc_cre_by_fk      ON transcription_comment(created_by);
CREATE INDEX trc_lst_mod_by_fk  ON transcription_comment(last_modified_by);

CREATE INDEX trd_tra_fk         ON transcription_document(tra_id);

CREATE INDEX trw_tra_fk         ON transcription_workflow(tra_id);
CREATE INDEX trw_trs_fk         ON transcription_workflow(trs_id);
CREATE INDEX trw_wrkflw_act_fk  ON transcription_workflow(workflow_actor);

CREATE INDEX trm_mer_fk         ON transformed_media(mer_id);

CREATE INDEX tod_cre_by_fk      ON transient_object_directory(created_by);
CREATE INDEX tod_lst_mod_by_fk  ON transient_object_directory(last_modified_by);
CREATE INDEX tod_trm_fk         ON transient_object_directory(trm_id);
CREATE INDEX tod_ors_fk         ON transient_object_directory(ors_id);

--v19 security
CREATE INDEX grp_rol_fk         ON security_group(rol_id);
CREATE INDEX grp_cre_by_fk      ON security_group(created_by);
CREATE INDEX grp_lst_mod_by_fk  ON security_group(last_modified_by);

--CREATE INDEX grc_grp_fk         On security_group_courthouse_ae(grp_id);
CREATE INDEX grc_cth_fk         ON security_group_courthouse_ae(cth_id);

CREATE INDEX gua_grp_fk         ON security_group_user_account_ae(grp_id);
--CREATE INDEX gua_usr_fk         On security_group_user_account_ae(usr_id);

--CREATE INDEX rop_rol_fk         On security_role_permission_ae(rol_id);
CREATE INDEX rop_per_fk         ON security_role_permission_ae(per_id);

--v6 retention
CREATE INDEX car_cas_fk         ON case_retention(cas_id);
CREATE INDEX car_rpt_fk         ON case_retention(rpt_id);
CREATE INDEX car_cre_by_fk      ON case_retention(created_by);
CREATE INDEX car_lst_mod_by_fk  ON case_retention(last_modified_by);
CREATE INDEX car_sub_by_fk      ON case_retention(submitted_by);
CREATE INDEX car_cmr_fk         ON case_retention(cmr_id);

CREATE INDEX cmr_cas_fk         ON case_management_retention(cas_id);
CREATE INDEX cmr_rpt_fk         ON case_management_retention(rpt_id);
CREATE INDEX cmr_eve_fk         ON case_management_retention(eve_id);

CREATE INDEX rpt_cre_by_fk      ON retention_policy_type(created_by);
CREATE INDEX rpt_lst_mod_by_fk  ON retention_policy_type(last_modified_by);


--v2
CREATE INDEX cas_cn_idx         ON court_case(case_number);
CREATE INDEX cth_cn_idx         ON courthouse(courthouse_name);
CREATE INDEX ctr_cn_idx         ON courtroom(courtroom_name);
CREATE INDEX dfc_dn_idx         ON defence(defence_name);
CREATE INDEX dfd_dn_idx         ON defendant(defendant_name);
CREATE INDEX hea_hd_idx         ON hearing(hearing_date);
CREATE INDEX jud_jn_idx         ON judge(judge_name);
CREATE INDEX prn_pn_idx         ON prosecutor(prosecutor_name);
CREATE INDEX usr_un_idx         ON user_account(user_name);

