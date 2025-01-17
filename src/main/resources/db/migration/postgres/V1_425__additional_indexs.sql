drop index event_event_id_is_current_idx;
create index request_status_mer_id_idx on media_request (request_status, mer_id);
create index created_ts_med_id_idx on media (created_ts, med_id);
create index event_status_idx on event (event_status);
create index hearing_date_ctr_id_idx on hearing (hearing_date, ctr_id);
create index cas_id_mlc_id_idx on media_linked_case (cas_id, mlc_id);
create index med_id_idx on media_linked_case (med_id);
create index source_idx on media_linked_case (source);
CREATE INDEX court_case_created_ts_idx ON court_case (created_ts, case_closed);

update user_account
set user_name ='system_DETSCleanupArmResponseFiles'
where user_email_address = 'systemDETSCleanupArmResponseFilesAutomatedTask@hmcts.net';

update user_account
set user_name ='system_ArmMissingResponseReplay'
where user_email_address = 'systemArmMissingResponseReplayAutomatedTask@hmcts.net';