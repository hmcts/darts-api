-- foreign keys

ALTER TABLE darts.annotation
    ADD CONSTRAINT annotation_case_fk
        FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.annotation
    ADD CONSTRAINT annotation_courtroom_fk
        FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.court_case
    ADD CONSTRAINT court_case_reporting_restriction_fk
        FOREIGN KEY (rer_id) REFERENCES darts.reporting_restrictions(rer_id);

ALTER TABLE darts.court_case
    ADD CONSTRAINT court_case_courthouse_fk
        FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.hearing
    ADD CONSTRAINT hearing_case_fk
        FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.courtroom
    ADD CONSTRAINT courtroom_courthouse_fk
        FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.daily_list
    ADD CONSTRAINT daily_list_courthouse_fk
        FOREIGN KEY (cth_id) REFERENCES darts.courthouse(cth_id);

ALTER TABLE darts.event
    ADD CONSTRAINT event_courtroom_fk
        FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.event
    ADD CONSTRAINT event_event_handler_fk
        FOREIGN KEY (evh_id) REFERENCES darts.event_handler(evh_id);

ALTER TABLE darts.external_object_directory
    ADD CONSTRAINT eod_media_fk
        FOREIGN KEY (med_id) REFERENCES darts.media(med_id);

ALTER TABLE darts.external_object_directory
    ADD CONSTRAINT eod_transcription_fk
        FOREIGN KEY (tra_id) REFERENCES darts.transcription(tra_id);

ALTER TABLE darts.external_object_directory
    ADD CONSTRAINT eod_annotation_fk
        FOREIGN KEY (ann_id) REFERENCES darts.annotation(ann_id);

ALTER TABLE darts.external_object_directory
    ADD CONSTRAINT eod_modified_by_fk
        FOREIGN KEY (modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.external_object_directory
    ADD CONSTRAINT eod_object_directory_status_fk
        FOREIGN KEY (ods_id) REFERENCES darts.object_directory_status(ods_id);

ALTER TABLE darts.external_object_directory
    ADD CONSTRAINT eod_external_location_type_fk
        FOREIGN KEY (elt_id) REFERENCES darts.external_location_type(elt_id);

ALTER TABLE darts.hearing
    ADD CONSTRAINT hearing_courtroom_fk
        FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.hearing_event_ae
    ADD CONSTRAINT hearing_event_ae_hearing_fk
        FOREIGN KEY (hea_id) REFERENCES darts.hearing(hea_id);

ALTER TABLE darts.hearing_event_ae
    ADD CONSTRAINT hearing_event_ae_event_fk
        FOREIGN KEY (eve_id) REFERENCES darts.event(eve_id);

ALTER TABLE darts.hearing_media_ae
    ADD CONSTRAINT hearing_media_ae_hearing_fk
        FOREIGN KEY (hea_id) REFERENCES darts.hearing(hea_id);

ALTER TABLE darts.hearing_media_ae
    ADD CONSTRAINT hearing_media_ae_media_fk
        FOREIGN KEY (med_id) REFERENCES darts.media(med_id);

ALTER TABLE darts.media
    ADD CONSTRAINT media_courtroom_fk
        FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.media_request
    ADD CONSTRAINT media_hearing_fk
        FOREIGN KEY (hea_id) REFERENCES darts.hearing(hea_id);

ALTER TABLE darts.notification
    ADD CONSTRAINT notification_case_fk
        FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_case_fk
        FOREIGN KEY (cas_id) REFERENCES darts.court_case(cas_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_courtroom_fk
        FOREIGN KEY (ctr_id) REFERENCES darts.courtroom(ctr_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_urgency_fk
        FOREIGN KEY (urg_id) REFERENCES darts.urgency(urg_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_last_modified_by_fk
        FOREIGN KEY (last_modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_requested_by_fk
        FOREIGN KEY (requested_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_approved_by_fk
        FOREIGN KEY (approved_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_transcribed_by_fk
        FOREIGN KEY (transcribed_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transcription
    ADD CONSTRAINT transcription_transcription_type_fk
        FOREIGN KEY (trt_id) REFERENCES darts.transcription_type(trt_id);

ALTER TABLE darts.transcription_comment
    ADD CONSTRAINT transcription_comment_transcription_fk
        FOREIGN KEY (tra_id) REFERENCES darts.transcription(tra_id);

ALTER TABLE darts.transcription_comment
    ADD CONSTRAINT transcription_comment_author_fk
        FOREIGN KEY (author) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transient_object_directory
    ADD CONSTRAINT tod_modified_by_fk
        FOREIGN KEY (modified_by) REFERENCES darts.user_account(usr_id);

ALTER TABLE darts.transient_object_directory
    ADD CONSTRAINT tod_media_request_fk
        FOREIGN KEY (mer_id) REFERENCES darts.media_request(mer_id);

ALTER TABLE darts.transient_object_directory
    ADD CONSTRAINT tod_object_directory_status_fk
        FOREIGN KEY (ods_id) REFERENCES darts.object_directory_status(ods_id);