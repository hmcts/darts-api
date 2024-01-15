SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO DARTS;

DROP TRIGGER trw_ar_trg ON transcription_workflow;
DROP FUNCTION tra_trw_sync_fnc;

DROP TABLE case_retention;
DROP TABLE case_management_retention;
DROP TABLE retention_policy_type;

DROP SEQUENCE cmr_seq;
DROP SEQUENCE car_seq;
DROP SEQUENCE rpt_seq;

DROP TABLE security_group_courthouse_ae;
DROP TABLE security_role_permission_ae;
DROP TABLE security_group_user_account_ae;
DROP TABLE security_permission;
DROP TABLE security_group;
DROP TABLE security_role;

DROP SEQUENCE grp_seq;
DROP SEQUENCE rol_seq;
DROP SEQUENCE per_seq;

DROP TABLE case_judge_ae;
DROP TABLE hearing_judge_ae;
DROP TABLE defence;
DROP TABLE defendant;
DROP TABLE prosecutor;
DROP TABLE judge;
DROP TABLE node_register;
DROP TABLE transient_object_directory;
DROP TABLE transformed_media;
DROP TABLE external_object_directory; 
DROP TABLE case_document; 
DROP TABLE object_record_status; 
DROP TABLE external_location_type;  
DROP TABLE annotation_document;
DROP TABLE hearing_annotation_ae;
DROP TABLE annotation ;
DROP TABLE media_request;

DROP TABLE hearing_event_ae;
DROP TABLE hearing_media_ae ;
DROP TABLE courthouse_region_ae;
DROP TABLE region;
DROP TABLE daily_list  ;              
DROP TABLE event       ; 

DROP TABLE transcription_document;
DROP TABLE transcription_comment ;
DROP TABLE transcription_workflow;
  
DROP TABLE transcription ; 
DROP TABLE transcription_urgency;
DROP TABLE hearing     ;              
DROP TABLE media       ;
DROP TABLE notification;              
DROP TABLE report      ;      
DROP TABLE automated_task;

DROP TABLE audit;
DROP TABLE audit_activity;
DROP TABLE external_service_auth_token;

DROP TABLE case_overflow;
DROP TABLE court_case;
DROP TABLE event_handler; 
DROP TABLE transcription_status;
DROP TABLE transcription_type;
DROP TABLE courtroom; 
DROP TABLE courthouse;   
DROP TABLE user_account;
 
DROP SEQUENCE ann_seq;
DROP SEQUENCE ado_seq;
DROP SEQUENCE aud_seq;
DROP SEQUENCE aua_seq;
DROP SEQUENCE aut_seq;
DROP SEQUENCE cad_seq;
DROP SEQUENCE cas_seq;
DROP SEQUENCE cth_seq;
DROP SEQUENCE ctr_seq;
DROP SEQUENCE dal_seq;
DROP SEQUENCE dfc_seq;
DROP SEQUENCE dfd_seq;
DROP SEQUENCE eve_seq;
DROP SEQUENCE evh_seq;
DROP SEQUENCE eod_seq;
DROP SEQUENCE elt_seq;
DROP SEQUENCE esa_seq;
DROP SEQUENCE jud_seq;
DROP SEQUENCE hea_seq;
DROP SEQUENCE med_seq;
DROP SEQUENCE mer_seq;
DROP SEQUENCE nod_seq;
DROP SEQUENCE not_seq;
DROP SEQUENCE ors_seq;
DROP SEQUENCE prn_seq;
DROP SEQUENCE reg_seq;
DROP SEQUENCE rep_seq;
DROP SEQUENCE tod_seq;
DROP SEQUENCE tra_seq;
DROP SEQUENCE trc_seq;
DROP SEQUENCE trd_seq;
DROP SEQUENCE trw_seq;
DROP SEQUENCE trm_seq;
DROP SEQUENCE usr_seq;




DROP SCHEMA darts;

SET ROLE POSTGRES;

DROP TABLESPACE darts_tables;
DROP TABLESPACE darts_indexes;

DROP USER darts_owner;
DROP USER darts_user;

