ALTER TABLE annotation              ADD CONSTRAINT annotation_pk PRIMARY KEY (ann_id);
ALTER TABLE case_judge_ae        ADD CONSTRAINT case_judge_ae_pk PRIMARY KEY (cas_id,jud_id);
ALTER TABLE case_retention          ADD CONSTRAINT case_retention_pk PRIMARY KEY (car_id);
ALTER TABLE case_retention_event    ADD CONSTRAINT case_retention_event_pk PRIMARY KEY (cre_id);
ALTER TABLE court_case              ADD CONSTRAINT court_case_pk PRIMARY KEY (cas_id);
ALTER TABLE courthouse              ADD CONSTRAINT courthouse_pk PRIMARY KEY (cth_id);
ALTER TABLE courthouse_region_ae    ADD CONSTRAINT courthouse_region_ae_pk PRIMARY KEY (cra_id);
ALTER TABLE courtroom               ADD CONSTRAINT courtroom_pk PRIMARY KEY (ctr_id);
ALTER TABLE daily_list              ADD CONSTRAINT daily_list_pk PRIMARY KEY (dal_id);
ALTER TABLE defence               ADD CONSTRAINT defence_pk PRIMARY KEY (dfc_id);
ALTER TABLE defendant             ADD CONSTRAINT defendant_pk PRIMARY KEY (dfd_id);
ALTER TABLE device_register         ADD CONSTRAINT device_register_pk PRIMARY KEY (der_id);
ALTER TABLE event                   ADD CONSTRAINT event_pk PRIMARY KEY (eve_id);
ALTER TABLE event_handler            ADD CONSTRAINT event_handler_pk PRIMARY KEY (evh_id);
ALTER TABLE external_object_directory   ADD CONSTRAINT external_object_directory_pk PRIMARY KEY (eod_id);
ALTER TABLE external_location_type   ADD CONSTRAINT external_location_type_pk PRIMARY KEY (elt_id);
ALTER TABLE hearing                 ADD CONSTRAINT hearing_pk PRIMARY KEY (hea_id);
ALTER TABLE hearing_event_ae        ADD CONSTRAINT hearing_event_ae_pk PRIMARY KEY (hea_id,eve_id);
ALTER TABLE hearing_judge_ae        ADD CONSTRAINT hearing_judge_ae_pk PRIMARY KEY (hea_id,jud_id);
ALTER TABLE hearing_media_ae        ADD CONSTRAINT hearing_media_ae_pk PRIMARY KEY (hea_id,med_id);
ALTER TABLE judge                ADD CONSTRAINT judge_pk PRIMARY KEY (jud_id);
ALTER TABLE media                   ADD CONSTRAINT media_pk PRIMARY KEY (med_id);
ALTER TABLE media_request           ADD CONSTRAINT media_request_pk PRIMARY KEY (mer_id);
ALTER TABLE notification            ADD CONSTRAINT notification_pk PRIMARY KEY (not_id);
ALTER TABLE object_directory_status ADD CONSTRAINT object_directory_status_pk PRIMARY KEY (ods_id);
ALTER TABLE prosecutor          ADD CONSTRAINT prosecutor_pk PRIMARY KEY (prn_id);
ALTER TABLE region                  ADD CONSTRAINT region_pk PRIMARY KEY (reg_id);
ALTER TABLE report                  ADD CONSTRAINT report_pk PRIMARY KEY (rep_id);
ALTER TABLE retention_policy           ADD CONSTRAINT retention_policy_pk PRIMARY KEY (rtp_id);
ALTER TABLE transcription           ADD CONSTRAINT transcription_pk PRIMARY KEY (tra_id);
ALTER TABLE transcription_comment   ADD CONSTRAINT transcription_comment_pk PRIMARY KEY (trc_id);
ALTER TABLE transcription_type      ADD CONSTRAINT transcription_type_pk PRIMARY KEY (trt_id);
ALTER TABLE transient_object_directory  ADD CONSTRAINT transient_object_directory_pk PRIMARY KEY (tod_id);
ALTER TABLE urgency                 ADD CONSTRAINT urgency_pk PRIMARY KEY (urg_id);
ALTER TABLE user_account            ADD CONSTRAINT user_account_pk PRIMARY KEY (usr_id);
