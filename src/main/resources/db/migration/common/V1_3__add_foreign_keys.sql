-- foreign keys

ALTER TABLE annotation
ADD CONSTRAINT annotation_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE annotation
ADD CONSTRAINT annotation_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE case_judge_ae
ADD CONSTRAINT case_judge_ae_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_judge_ae
ADD CONSTRAINT case_judge_ae_judge_fk
FOREIGN KEY (jud_id) REFERENCES judge(jud_id);

ALTER TABLE case_retention
ADD CONSTRAINT case_retention_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_retention
ADD CONSTRAINT case_retention_retention_policy_fk
FOREIGN KEY (rtp_id) REFERENCES retention_policy(rtp_id);

ALTER TABLE case_retention_event
ADD CONSTRAINT case_retention_event_case_retention_fk
FOREIGN KEY (car_id) REFERENCES case_retention(car_id);

ALTER TABLE court_case
ADD CONSTRAINT court_case_event_handler_fk
FOREIGN KEY (evh_id) REFERENCES event_handler(evh_id);

ALTER TABLE court_case
ADD CONSTRAINT court_case_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE courthouse_region_ae
ADD CONSTRAINT courthouse__region_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE courthouse_region_ae
ADD CONSTRAINT courthouse__region_region_fk
FOREIGN KEY (reg_id) REFERENCES region(reg_id);

ALTER TABLE hearing
ADD CONSTRAINT hearing_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE courtroom
ADD CONSTRAINT courtroom_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE daily_list
ADD CONSTRAINT daily_list_courthouse_fk
FOREIGN KEY (cth_id) REFERENCES courthouse(cth_id);

ALTER TABLE defence
ADD CONSTRAINT defence_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE defendant
ADD CONSTRAINT defendant_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE device_register
ADD CONSTRAINT device_register_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE event
ADD CONSTRAINT event_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE event
ADD CONSTRAINT event_event_handler_fk
FOREIGN KEY (evh_id) REFERENCES event_handler(evh_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT eod_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT eod_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT eod_annotation_fk
FOREIGN KEY (ann_id) REFERENCES annotation(ann_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT eod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT eod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES object_directory_status(ods_id);

ALTER TABLE external_object_directory
ADD CONSTRAINT eod_external_location_type_fk
FOREIGN KEY (elt_id) REFERENCES external_location_type(elt_id);

ALTER TABLE hearing
ADD CONSTRAINT hearing_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE hearing_event_ae
ADD CONSTRAINT hearing_event_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_event_ae
ADD CONSTRAINT hearing_event_ae_event_fk
FOREIGN KEY (eve_id) REFERENCES event(eve_id);

ALTER TABLE hearing_judge_ae
ADD CONSTRAINT hearing_judge_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_judge_ae
ADD CONSTRAINT hearing_judge_ae_judge_fk
FOREIGN KEY (jud_id) REFERENCES judge(jud_id);

ALTER TABLE hearing_media_ae
ADD CONSTRAINT hearing_media_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_media_ae
ADD CONSTRAINT hearing_media_ae_media_fk
FOREIGN KEY (med_id) REFERENCES media(med_id);

ALTER TABLE media
ADD CONSTRAINT media_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE media_request
ADD CONSTRAINT media_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE notification
ADD CONSTRAINT notification_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE prosecutor
ADD CONSTRAINT prosecutor_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_courtroom_fk
FOREIGN KEY (ctr_id) REFERENCES courtroom(ctr_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_urgency_fk
FOREIGN KEY (urg_id) REFERENCES urgency(urg_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_requested_by_fk
FOREIGN KEY (requested_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_approved_by_fk
FOREIGN KEY (approved_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_transcribed_by_fk
FOREIGN KEY (transcribed_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription
ADD CONSTRAINT transcription_transcription_type_fk
FOREIGN KEY (trt_id) REFERENCES transcription_type(trt_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_author_fk
FOREIGN KEY (author) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory
ADD CONSTRAINT tod_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transient_object_directory
ADD CONSTRAINT tod_media_request_fk
FOREIGN KEY (mer_id) REFERENCES media_request(mer_id);

ALTER TABLE transient_object_directory
ADD CONSTRAINT tod_object_directory_status_fk
FOREIGN KEY (ods_id) REFERENCES object_directory_status(ods_id);

