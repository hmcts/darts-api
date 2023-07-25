-- primary keys

CREATE UNIQUE INDEX annotation_pk ON darts.annotation(ann_id);
ALTER  TABLE darts.annotation              ADD PRIMARY KEY USING INDEX annotation_pk;

CREATE UNIQUE INDEX court_case_pk ON darts.court_case(cas_id);
ALTER  TABLE darts.court_case              ADD PRIMARY KEY USING INDEX court_case_pk;

CREATE UNIQUE INDEX courthouse_pk ON darts.courthouse(cth_id);
ALTER  TABLE darts.courthouse              ADD PRIMARY KEY USING INDEX courthouse_pk;

CREATE UNIQUE INDEX courthouse_region_ae_pk ON darts.courthouse_region_ae(cra_id);
ALTER  TABLE darts.courthouse_region_ae    ADD PRIMARY KEY USING INDEX courthouse_region_ae_pk;

CREATE UNIQUE INDEX courtroom_pk ON darts.courtroom(ctr_id);
ALTER  TABLE darts.courtroom               ADD PRIMARY KEY USING INDEX courtroom_pk;

CREATE UNIQUE INDEX daily_list_pk ON darts.daily_list(dal_id);
ALTER  TABLE darts.daily_list              ADD PRIMARY KEY USING INDEX daily_list_pk;

CREATE UNIQUE INDEX device_register_pk ON darts.device_register(der_id);
ALTER  TABLE darts.device_register              ADD PRIMARY KEY USING INDEX device_register_pk;

CREATE UNIQUE INDEX event_pk ON darts.event(eve_id);
ALTER  TABLE darts.event                   ADD PRIMARY KEY USING INDEX event_pk;

CREATE UNIQUE INDEX event_handler_pk ON darts.event_handler(evh_id);
ALTER  TABLE darts.event_handler              ADD PRIMARY KEY USING INDEX event_handler_pk;

CREATE UNIQUE INDEX external_object_directory_pk ON darts.external_object_directory(eod_id);
ALTER  TABLE darts.external_object_directory   ADD PRIMARY KEY USING INDEX external_object_directory_pk;

CREATE UNIQUE INDEX external_location_type_pk ON darts.external_location_type(elt_id);
ALTER  TABLE darts.external_location_type   ADD PRIMARY KEY USING INDEX external_location_type_pk;

CREATE UNIQUE INDEX hearing_pk ON darts.hearing(hea_id);
ALTER  TABLE darts.hearing                 ADD PRIMARY KEY USING INDEX hearing_pk;

CREATE UNIQUE INDEX hearing_event_ae_pk ON darts.hearing_event_ae(hev_id);
ALTER  TABLE darts.hearing_event_ae        ADD PRIMARY KEY USING INDEX hearing_event_ae_pk;

CREATE UNIQUE INDEX hearing_media_ae_pk ON darts.hearing_media_ae(hma_id);
ALTER  TABLE darts.hearing_media_ae        ADD PRIMARY KEY USING INDEX hearing_media_ae_pk;

CREATE UNIQUE INDEX media_pk ON darts.media(med_id);
ALTER  TABLE darts.media                   ADD PRIMARY KEY USING INDEX media_pk;

CREATE UNIQUE INDEX media_request_pk ON darts.media_request(mer_id);
ALTER  TABLE darts.media_request           ADD PRIMARY KEY USING INDEX media_request_pk;

CREATE UNIQUE INDEX notification_pk ON darts.notification(not_id);
ALTER  TABLE darts.notification            ADD PRIMARY KEY USING INDEX notification_pk;

CREATE UNIQUE INDEX object_directory_status_pk ON darts.object_directory_status(ods_id);
ALTER  TABLE darts.object_directory_status ADD PRIMARY KEY USING INDEX object_directory_status_pk;

CREATE UNIQUE INDEX region_pk ON darts.region(reg_id);
ALTER  TABLE darts.region                  ADD PRIMARY KEY USING INDEX region_pk;

CREATE UNIQUE INDEX report_pk ON darts.report(rep_id);
ALTER  TABLE darts.report                  ADD PRIMARY KEY USING INDEX report_pk;

CREATE UNIQUE INDEX transcription_pk ON darts.transcription(tra_id);
ALTER  TABLE darts.transcription           ADD PRIMARY KEY USING INDEX transcription_pk;

CREATE UNIQUE INDEX transcription_comment_pk ON darts.transcription_comment(trc_id);
ALTER  TABLE darts.transcription_comment   ADD PRIMARY KEY USING INDEX transcription_comment_pk;

CREATE UNIQUE INDEX transcription_type_pk ON darts.transcription_type(trt_id);
ALTER  TABLE darts.transcription_type      ADD PRIMARY KEY USING INDEX transcription_type_pk;

CREATE UNIQUE INDEX transient_object_directory_pk ON darts.transient_object_directory(tod_id);
ALTER  TABLE darts.transient_object_directory  ADD PRIMARY KEY USING INDEX transient_object_directory_pk;

CREATE UNIQUE INDEX urgency_pk ON darts.urgency(urg_id);
ALTER  TABLE darts.urgency                 ADD PRIMARY KEY USING INDEX urgency_pk;

CREATE UNIQUE INDEX user_account_pk ON darts.user_account( usr_id);
ALTER  TABLE darts.user_account            ADD PRIMARY KEY USING INDEX user_account_pk;
