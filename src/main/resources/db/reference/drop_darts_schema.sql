SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO DARTS;

DROP TABLE security_group_courthouse;
DROP TABLE security_role_permission;
DROP TABLE security_group_membership_ae;
DROP TABLE security_permission;
DROP TABLE security_group;
DROP TABLE security_role;

DROP TABLE defence_name;
DROP TABLE defendant_name;
DROP TABLE prosecutor_name;
DROP TABLE judge_name;
DROP TABLE device_register;
DROP TABLE transient_object_directory;
DROP TABLE external_object_directory;
DROP TABLE object_directory_status;
DROP TABLE external_location_type;
DROP TABLE annotation ;
DROP TABLE media_request;
DROP TABLE hearing_event_ae;
DROP TABLE hearing_media_ae ;
DROP TABLE courthouse_region_ae;
DROP TABLE region;
DROP TABLE daily_list  ;
DROP TABLE event       ;
DROP TABLE event_handler;
DROP TABLE hearing     ;
DROP TABLE media       ;
DROP TABLE notification;
DROP TABLE report      ;
DROP TABLE transcription_comment ;
DROP TABLE transcription ;
DROP TABLE urgency;
DROP TABLE user_account;
DROP TABLE court_case;
DROP TABLE reporting_restriction;
DROP TABLE transcription_type;
DROP TABLE courtroom;
DROP TABLE courthouse ;

DROP SEQUENCE ann_seq;
DROP SEQUENCE cas_seq;
DROP SEQUENCE cra_seq;
DROP SEQUENCE cth_seq;
DROP SEQUENCE ctr_seq;
DROP SEQUENCE dal_seq;
DROP SEQUENCE dfc_seq;
DROP SEQUENCE dfd_seq;
DROP SEQUENCE der_seq;
DROP SEQUENCE eve_seq;
DROP SEQUENCE evh_seq;
DROP SEQUENCE eod_seq;
DROP SEQUENCE elt_seq;
DROP SEQUENCE jud_seq;
DROP SEQUENCE hea_seq;
DROP SEQUENCE med_seq;
DROP SEQUENCE mer_seq;
DROP SEQUENCE not_seq;
DROP SEQUENCE ods_seq;
DROP SEQUENCE prn_seq;
DROP SEQUENCE reg_seq;
DROP SEQUENCE rep_seq;
DROP SEQUENCE rer_seq;
DROP SEQUENCE tod_seq;
DROP SEQUENCE tra_seq;
DROP SEQUENCE trc_seq;
DROP SEQUENCE trt_seq;
DROP SEQUENCE urg_seq;
DROP SEQUENCE usr_seq;

DROP SCHEMA darts;

SET ROLE POSTGRES;

DROP TABLESPACE darts_tables;
DROP TABLESPACE darts_indexes;

DROP USER darts_owner;
DROP USER darts_user;

