drop index concurrently "darts"."event_event_id_is_current_idx";
create index concurrently request_status_mer_id_idx on darts.media_request (request_status, mer_id);
create index concurrently created_ts_med_id_idx on darts.media (created_ts, med_id);
create index concurrently event_status_idx on darts.event (event_status);
create index concurrently hearing_date_ctr_id_idx on darts.hearing (hearing_date, ctr_id);
create index concurrently cas_id_mlc_id_idx on darts.media_linked_case (cas_id, mlc_id);
create index concurrently med_id_idx on darts.media_linked_case (med_id);
create index concurrently source_idx on darts.media_linked_case (source);
CREATE INDEX court_case_created_ts_idx ON darts.court_case (created_ts, case_closed);

update darts.user_account
set user_name ='system_DETSCleanupArmResponseFiles'
where user_email_address = 'systemDETSCleanupArmResponseFilesAutomatedTask@hmcts.net';

update darts.user_account
set user_name ='system_ArmMissingResponseReplay'
where user_email_address = 'systemArmMissingResponseReplayAutomatedTask@hmcts.net';