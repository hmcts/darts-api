-- primary keys

CREATE UNIQUE INDEX annotation_pk ON annotation(ann_id);
ALTER TABLE annotation              ADD PRIMARY KEY USING INDEX annotation_pk;

CREATE UNIQUE INDEX automated_task_pk ON automated_task(aut_id);
ALTER TABLE automated_task          ADD PRIMARY KEY USING INDEX automated_task_pk;

CREATE UNIQUE INDEX case_judge_ae_pk ON case_judge_ae(cas_id,jud_id);
ALTER TABLE case_judge_ae        ADD PRIMARY KEY USING INDEX case_judge_ae_pk;

CREATE UNIQUE INDEX case_retention_pk ON case_retention(car_id);
ALTER TABLE case_retention          ADD PRIMARY KEY USING INDEX case_retention_pk;

CREATE UNIQUE INDEX case_retention_event_pk ON case_retention_event(cre_id);
ALTER TABLE case_retention_event    ADD PRIMARY KEY USING INDEX case_retention_event_pk;

CREATE UNIQUE INDEX court_case_pk ON court_case(cas_id);
ALTER TABLE court_case              ADD PRIMARY KEY USING INDEX court_case_pk;

CREATE UNIQUE INDEX courthouse_pk ON courthouse(cth_id);
ALTER TABLE courthouse              ADD PRIMARY KEY USING INDEX courthouse_pk;

CREATE UNIQUE INDEX courthouse_region_ae_pk ON courthouse_region_ae(cra_id);
ALTER TABLE courthouse_region_ae    ADD PRIMARY KEY USING INDEX courthouse_region_ae_pk;

CREATE UNIQUE INDEX courtroom_pk ON courtroom(ctr_id);
ALTER TABLE courtroom               ADD PRIMARY KEY USING INDEX courtroom_pk;

CREATE UNIQUE INDEX daily_list_pk ON daily_list(dal_id);
ALTER TABLE daily_list              ADD PRIMARY KEY USING INDEX daily_list_pk;

CREATE UNIQUE INDEX defence_pk    ON defence(dfc_id);
ALTER TABLE defence               ADD PRIMARY KEY USING INDEX defence_pk;

CREATE UNIQUE INDEX defendant_pk ON defendant(dfd_id);
ALTER TABLE defendant             ADD PRIMARY KEY USING INDEX defendant_pk;

CREATE UNIQUE INDEX device_register_pk ON device_register(der_id);
ALTER TABLE device_register         ADD PRIMARY KEY USING INDEX device_register_pk;

CREATE UNIQUE INDEX event_pk ON event(eve_id);
ALTER TABLE event                   ADD PRIMARY KEY USING INDEX event_pk;

CREATE UNIQUE INDEX event_handler_pk ON event_handler(evh_id);
ALTER TABLE event_handler            ADD PRIMARY KEY USING INDEX event_handler_pk;

CREATE UNIQUE INDEX external_object_directory_pk ON external_object_directory(eod_id);
ALTER TABLE external_object_directory   ADD PRIMARY KEY USING INDEX external_object_directory_pk;

CREATE UNIQUE INDEX external_location_type_pk ON external_location_type(elt_id);
ALTER TABLE external_location_type   ADD PRIMARY KEY USING INDEX external_location_type_pk;

CREATE UNIQUE INDEX hearing_pk ON hearing(hea_id);
ALTER TABLE hearing                 ADD PRIMARY KEY USING INDEX hearing_pk;

CREATE UNIQUE INDEX hearing_event_ae_pk ON hearing_event_ae(hea_id,eve_id);
ALTER TABLE hearing_event_ae        ADD PRIMARY KEY USING INDEX hearing_event_ae_pk;

CREATE UNIQUE INDEX hearing_judge_ae_pk ON hearing_judge_ae(hea_id,jud_id);
ALTER TABLE hearing_judge_ae        ADD PRIMARY KEY USING INDEX hearing_judge_ae_pk;

CREATE UNIQUE INDEX hearing_media_ae_pk ON hearing_media_ae(hea_id,med_id);
ALTER TABLE hearing_media_ae        ADD PRIMARY KEY USING INDEX hearing_media_ae_pk;

CREATE UNIQUE INDEX judge_pk     ON judge(jud_id);
ALTER TABLE judge                ADD PRIMARY KEY USING INDEX judge_pk;

CREATE UNIQUE INDEX media_pk ON media(med_id);
ALTER TABLE media                   ADD PRIMARY KEY USING INDEX media_pk;

CREATE UNIQUE INDEX media_request_pk ON media_request(mer_id);
ALTER TABLE media_request           ADD PRIMARY KEY USING INDEX media_request_pk;

CREATE UNIQUE INDEX notification_pk ON notification(not_id);
ALTER TABLE notification            ADD PRIMARY KEY USING INDEX notification_pk;

CREATE UNIQUE INDEX object_directory_status_pk ON object_directory_status(ods_id);
ALTER TABLE object_directory_status ADD PRIMARY KEY USING INDEX object_directory_status_pk;

CREATE UNIQUE INDEX prosecutor_pk ON prosecutor(prn_id);
ALTER TABLE prosecutor          ADD PRIMARY KEY USING INDEX prosecutor_pk;

CREATE UNIQUE INDEX region_pk ON region(reg_id);
ALTER TABLE region                  ADD PRIMARY KEY USING INDEX region_pk;

CREATE UNIQUE INDEX report_pk ON report(rep_id);
ALTER TABLE report                  ADD PRIMARY KEY USING INDEX report_pk;

CREATE UNIQUE INDEX retention_policy_pk ON retention_policy(rtp_id);
ALTER TABLE retention_policy           ADD PRIMARY KEY USING INDEX retention_policy_pk;

CREATE UNIQUE INDEX transcription_pk ON transcription(tra_id);
ALTER TABLE transcription           ADD PRIMARY KEY USING INDEX transcription_pk;

CREATE UNIQUE INDEX transcription_comment_pk ON transcription_comment(trc_id);
ALTER TABLE transcription_comment   ADD PRIMARY KEY USING INDEX transcription_comment_pk;

CREATE UNIQUE INDEX transcription_type_pk ON transcription_type(trt_id);
ALTER TABLE transcription_type      ADD PRIMARY KEY USING INDEX transcription_type_pk;

CREATE UNIQUE INDEX transient_object_directory_pk ON transient_object_directory(tod_id);
ALTER TABLE transient_object_directory  ADD PRIMARY KEY USING INDEX transient_object_directory_pk;

CREATE UNIQUE INDEX urgency_pk ON urgency(urg_id);
ALTER TABLE urgency                 ADD PRIMARY KEY USING INDEX urgency_pk;

CREATE UNIQUE INDEX user_account_pk ON user_account( usr_id);
ALTER TABLE user_account            ADD PRIMARY KEY USING INDEX user_account_pk;
