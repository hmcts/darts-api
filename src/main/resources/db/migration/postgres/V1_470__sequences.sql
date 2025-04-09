SELECT setval('darts.aat_seq', COALESCE((SELECT CASE WHEN MAX(aat_id) < 0 THEN NULL ELSE MAX(aat_id) END FROM darts.arm_automated_task), 1), true);
SELECT setval('darts.ado_seq', COALESCE((SELECT CASE WHEN MAX(ado_id) < 0 THEN NULL ELSE MAX(ado_id) END FROM darts.annotation_document), 1), true);
SELECT setval('darts.ann_seq', COALESCE((SELECT CASE WHEN MAX(ann_id) < 0 THEN NULL ELSE MAX(ann_id) END FROM darts.annotation), 1), true);
SELECT setval('darts.ard_seq', COALESCE((SELECT CASE WHEN MAX(ard_id) < 0 THEN NULL ELSE MAX(ard_id) END FROM darts.arm_rpo_execution_detail), 1), true);
SELECT setval('darts.are_seq', COALESCE((SELECT CASE WHEN MAX(are_id) < 0 THEN NULL ELSE MAX(are_id) END FROM darts.arm_rpo_state), 1), true);
SELECT setval('darts.aru_seq', COALESCE((SELECT CASE WHEN MAX(aru_id) < 0 THEN NULL ELSE MAX(aru_id) END FROM darts.arm_rpo_status), 1), true);
SELECT setval('darts.aua_seq', COALESCE((SELECT CASE WHEN MAX(aua_id) < 0 THEN NULL ELSE MAX(aua_id) END FROM darts.audit_activity), 1), true);
SELECT setval('darts.aud_seq', COALESCE((SELECT CASE WHEN MAX(aud_id) < 0 THEN NULL ELSE MAX(aud_id) END FROM darts.audit), 1), true);
SELECT setval('darts.aut_seq', COALESCE((SELECT CASE WHEN MAX(aut_id) < 0 THEN NULL ELSE MAX(aut_id) END FROM darts.automated_task), 1), true);
SELECT setval('darts.cad_seq', COALESCE((SELECT CASE WHEN MAX(cad_id) < 0 THEN NULL ELSE MAX(cad_id) END FROM darts.case_document), 1), true);
SELECT setval('darts.car_seq', COALESCE((SELECT CASE WHEN MAX(car_id) < 0 THEN NULL ELSE MAX(car_id) END FROM darts.case_retention), 1), true);
SELECT setval('darts.cas_seq', COALESCE((SELECT CASE WHEN MAX(cas_id) < 0 THEN NULL ELSE MAX(cas_id) END FROM darts.court_case), 1), true);
SELECT setval('darts.cmr_seq', COALESCE((SELECT CASE WHEN MAX(cmr_id) < 0 THEN NULL ELSE MAX(cmr_id) END FROM darts.case_management_retention), 1), true);
SELECT setval('darts.cof_seq', COALESCE((SELECT CASE WHEN MAX(cof_id) < 0 THEN NULL ELSE MAX(cof_id) END FROM darts.case_overflow), 1), true);
SELECT setval('darts.cth_seq', COALESCE((SELECT CASE WHEN MAX(cth_id) < 0 THEN NULL ELSE MAX(cth_id) END FROM darts.courthouse), 1), true);
SELECT setval('darts.ctr_seq', COALESCE((SELECT CASE WHEN MAX(ctr_id) < 0 THEN NULL ELSE MAX(ctr_id) END FROM darts.courtroom), 1), true);
SELECT setval('darts.dal_seq', COALESCE((SELECT CASE WHEN MAX(dal_id) < 0 THEN NULL ELSE MAX(dal_id) END FROM darts.daily_list), 1), true);
SELECT setval('darts.dan_seq', COALESCE((SELECT CASE WHEN MAX(dan_id) < 0 THEN NULL ELSE MAX(dan_id) END FROM darts.data_anonymisation), 1), true);
SELECT setval('darts.dfc_seq', COALESCE((SELECT CASE WHEN MAX(dfc_id) < 0 THEN NULL ELSE MAX(dfc_id) END FROM darts.defence), 1), true);
SELECT setval('darts.dfd_seq', COALESCE((SELECT CASE WHEN MAX(dfd_id) < 0 THEN NULL ELSE MAX(dfd_id) END FROM darts.defendant), 1), true);
SELECT setval('darts.elc_seq', COALESCE((SELECT CASE WHEN MAX(elc_id) < 0 THEN NULL ELSE MAX(elc_id) END FROM darts.event_linked_case), 1), true);
SELECT setval('darts.elt_seq', COALESCE((SELECT CASE WHEN MAX(elt_id) < 0 THEN NULL ELSE MAX(elt_id) END FROM darts.external_location_type), 1), true);
SELECT setval('darts.eod_seq', COALESCE((SELECT CASE WHEN MAX(eod_id) < 0 THEN NULL ELSE MAX(eod_id) END FROM darts.external_object_directory), 1), true);
SELECT setval('darts.epd_seq', COALESCE((SELECT CASE WHEN MAX(epd_id) < 0 THEN NULL ELSE MAX(epd_id) END FROM darts.extobjdir_process_detail), 1), true);
SELECT setval('darts.esa_seq', COALESCE((SELECT CASE WHEN MAX(esa_id) < 0 THEN NULL ELSE MAX(esa_id) END FROM darts.external_service_auth_token), 1), true);
SELECT setval('darts.eve_seq', COALESCE((SELECT CASE WHEN MAX(eve_id) < 0 THEN NULL ELSE MAX(eve_id) END FROM darts.event), 1), true);
SELECT setval('darts.evh_seq', COALESCE((SELECT CASE WHEN MAX(evh_id) < 0 THEN NULL ELSE MAX(evh_id) END FROM darts.event_handler), 1), true);
SELECT setval('darts.grp_seq', COALESCE((SELECT CASE WHEN MAX(grp_id) < 0 THEN NULL ELSE MAX(grp_id) END FROM darts.security_group), 1), true);
SELECT setval('darts.hea_seq', COALESCE((SELECT CASE WHEN MAX(hea_id) < 0 THEN NULL ELSE MAX(hea_id) END FROM darts.hearing), 1), true);
SELECT setval('darts.jud_seq', COALESCE((SELECT CASE WHEN MAX(jud_id) < 0 THEN NULL ELSE MAX(jud_id) END FROM darts.judge), 1), true);
SELECT setval('darts.med_seq', COALESCE((SELECT CASE WHEN MAX(med_id) < 0 THEN NULL ELSE MAX(med_id) END FROM darts.media), 1), true);
SELECT setval('darts.mer_seq', COALESCE((SELECT CASE WHEN MAX(mer_id) < 0 THEN NULL ELSE MAX(mer_id) END FROM darts.media_request), 1), true);
SELECT setval('darts.mlc_seq', COALESCE((SELECT CASE WHEN MAX(mlc_id) < 0 THEN NULL ELSE MAX(mlc_id) END FROM darts.media_linked_case), 1), true);
SELECT setval('darts.nod_seq', COALESCE((SELECT CASE WHEN MAX(node_id) < 0 THEN NULL ELSE MAX(node_id) END FROM darts.node_register), 1), true);
SELECT setval('darts.not_seq', COALESCE((SELECT CASE WHEN MAX(not_id) < 0 THEN NULL ELSE MAX(not_id) END FROM darts.notification), 1), true);
SELECT setval('darts.oaa_seq', COALESCE((SELECT CASE WHEN MAX(oaa_id) < 0 THEN NULL ELSE MAX(oaa_id) END FROM darts.object_admin_action), 1), true);
SELECT setval('darts.ohr_seq', COALESCE((SELECT CASE WHEN MAX(ohr_id) < 0 THEN NULL ELSE MAX(ohr_id) END FROM darts.object_hidden_reason), 1), true);
SELECT setval('darts.orq_seq', COALESCE((SELECT CASE WHEN MAX(orq_id) < 0 THEN NULL ELSE MAX(orq_id) END FROM darts.object_retrieval_queue), 1), true);
SELECT setval('darts.ors_seq', COALESCE((SELECT CASE WHEN MAX(ors_id) < 0 THEN NULL ELSE MAX(ors_id) END FROM darts.object_record_status), 1), true);
SELECT setval('darts.per_seq', COALESCE((SELECT CASE WHEN MAX(per_id) < 0 THEN NULL ELSE MAX(per_id) END FROM darts.security_permission), 1), true);
SELECT setval('darts.prn_seq', COALESCE((SELECT CASE WHEN MAX(prn_id) < 0 THEN NULL ELSE MAX(prn_id) END FROM darts.prosecutor), 1), true);
SELECT setval('darts.rah_seq', COALESCE((SELECT CASE WHEN MAX(rah_id) < 0 THEN NULL ELSE MAX(rah_id) END FROM darts.case_retention_audit_heritage), 1), true);
SELECT setval('darts.rcc_seq', COALESCE((SELECT CASE WHEN MAX(rcc_id) < 0 THEN NULL ELSE MAX(rcc_id) END FROM darts.retention_confidence_category_mapper), 1), true);
SELECT setval('darts.reg_seq', COALESCE((SELECT CASE WHEN MAX(reg_id) < 0 THEN NULL ELSE MAX(reg_id) END FROM darts.region), 1), true);
SELECT setval('darts.rep_seq', COALESCE((SELECT CASE WHEN MAX(rep_id) < 0 THEN NULL ELSE MAX(rep_id) END FROM darts.report), 1), true);
SELECT setval('darts.revinfo_seq', COALESCE((SELECT CASE WHEN MAX(rev) < 0 THEN NULL ELSE MAX(rev) END FROM darts.revinfo), 1), true);
SELECT setval('darts.rhm_seq', COALESCE((SELECT CASE WHEN MAX(rhm_id) < 0 THEN NULL ELSE MAX(rhm_id) END FROM darts.retention_policy_type_heritage_mapping), 1), true);
SELECT setval('darts.rol_seq', COALESCE((SELECT CASE WHEN MAX(rol_id) < 0 THEN NULL ELSE MAX(rol_id) END FROM darts.security_role), 1), true);
SELECT setval('darts.rpr_seq', COALESCE((SELECT CASE WHEN MAX(rpr_id) < 0 THEN NULL ELSE MAX(rpr_id) END FROM darts.rps_retainer), 1), true);
SELECT setval('darts.rpt_seq', COALESCE((SELECT CASE WHEN MAX(rpt_id) < 0 THEN NULL ELSE MAX(rpt_id) END FROM darts.retention_policy_type), 1), true);
SELECT setval('darts.tlc_seq', COALESCE((SELECT CASE WHEN MAX(tlc_id) < 0 THEN NULL ELSE MAX(tlc_id) END FROM darts.transcription_linked_case), 1), true);
SELECT setval('darts.tod_seq', COALESCE((SELECT CASE WHEN MAX(tod_id) < 0 THEN NULL ELSE MAX(tod_id) END FROM darts.transient_object_directory), 1), true);
SELECT setval('darts.tra_seq', COALESCE((SELECT CASE WHEN MAX(tra_id) < 0 THEN NULL ELSE MAX(tra_id) END FROM darts.transcription), 1), true);
SELECT setval('darts.trc_seq', COALESCE((SELECT CASE WHEN MAX(trc_id) < 0 THEN NULL ELSE MAX(trc_id) END FROM darts.transcription_comment), 1), true);
SELECT setval('darts.trd_seq', COALESCE((SELECT CASE WHEN MAX(trd_id) < 0 THEN NULL ELSE MAX(trd_id) END FROM darts.transcription_document), 1), true);
SELECT setval('darts.trm_seq', COALESCE((SELECT CASE WHEN MAX(trm_id) < 0 THEN NULL ELSE MAX(trm_id) END FROM darts.transformed_media), 1), true);
SELECT setval('darts.trw_seq', COALESCE((SELECT CASE WHEN MAX(trw_id) < 0 THEN NULL ELSE MAX(trw_id) END FROM darts.transcription_workflow), 1), true);
SELECT setval('darts.usr_seq', COALESCE((SELECT CASE WHEN MAX(usr_id) < 0 THEN NULL ELSE MAX(usr_id) END FROM darts.user_account), 1), true);