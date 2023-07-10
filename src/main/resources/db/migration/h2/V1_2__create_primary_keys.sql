-- primary keys

ALTER TABLE darts.annotation ADD CONSTRAINT annotation_pk PRIMARY KEY(ann_id);
ALTER TABLE darts.court_case ADD CONSTRAINT court_case_pk PRIMARY KEY(cas_id);
ALTER TABLE darts.courthouse ADD CONSTRAINT courthouse_pk PRIMARY KEY(cth_id);
ALTER TABLE darts.courtroom ADD CONSTRAINT courtroom_pk PRIMARY KEY(ctr_id);
ALTER TABLE darts.daily_list ADD CONSTRAINT daily_list_pk PRIMARY KEY(dal_id);
ALTER TABLE darts.event ADD CONSTRAINT event_pk PRIMARY KEY(eve_id);
ALTER TABLE darts.event_handler ADD CONSTRAINT event_handler_pk PRIMARY KEY(evh_id);
ALTER TABLE darts.external_object_directory ADD CONSTRAINT external_object_directory_pk PRIMARY KEY(eod_id);
ALTER TABLE darts.external_location_type ADD CONSTRAINT external_location_type_pk PRIMARY KEY(elt_id);
ALTER TABLE darts.hearing ADD CONSTRAINT hearing_pk PRIMARY KEY(hea_id);
ALTER TABLE darts.hearing_event_ae ADD CONSTRAINT hearing_event_ae_pk PRIMARY KEY(hev_id);
ALTER TABLE darts.hearing_media_ae ADD CONSTRAINT hearing_media_ae_pk PRIMARY KEY(hma_id);
ALTER TABLE darts.media ADD CONSTRAINT media_pk PRIMARY KEY(med_id);
ALTER TABLE darts.media_request ADD CONSTRAINT media_request_pk PRIMARY KEY(mer_id);
ALTER TABLE darts.notification ADD CONSTRAINT notification_pk PRIMARY KEY(not_id);
ALTER TABLE darts.object_directory_status ADD CONSTRAINT object_directory_status_pk PRIMARY KEY(ods_id);
ALTER TABLE darts.report ADD CONSTRAINT report_pk PRIMARY KEY(rep_id);
ALTER TABLE darts.reporting_restrictions ADD CONSTRAINT reporting_restrictons_pk PRIMARY KEY(rer_id);
ALTER TABLE darts.transcription ADD CONSTRAINT transcription_pk PRIMARY KEY(tra_id);
ALTER TABLE darts.transcription_comment ADD CONSTRAINT transcription_comment_pk PRIMARY KEY(trc_id);
ALTER TABLE darts.transcription_type ADD CONSTRAINT transcription_type_pk PRIMARY KEY(trt_id);
ALTER TABLE darts.transient_object_directory ADD CONSTRAINT transient_object_directory_pk PRIMARY KEY(tod_id);
ALTER TABLE darts.urgency ADD CONSTRAINT urgency_pk PRIMARY KEY(urg_id);
ALTER TABLE darts.user_account ADD CONSTRAINT user_account_pk PRIMARY KEY(usr_id);

