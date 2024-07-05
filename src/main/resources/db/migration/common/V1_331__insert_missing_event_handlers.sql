insert into event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, created_ts, is_reporting_restriction)
    (select nextval('evh_seq'), '0', '0', 'Bench warrant', 'StandardEventHandler', false, current_timestamp, false);

insert into event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, created_ts, is_reporting_restriction)
    (select nextval('evh_seq'), '0', '0', 'Sentencing - Life', 'StandardEventHandler', false, current_timestamp, false);

insert into event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, created_ts, is_reporting_restriction)
    (select nextval('evh_seq'), '40734', NULL, 'Verdict', 'StandardEventHandler', true, current_timestamp, false);

insert into event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, created_ts, is_reporting_restriction)
    (select nextval('evh_seq'), '40735', NULL, 'Verdict', 'StandardEventHandler', true, current_timestamp, false);

