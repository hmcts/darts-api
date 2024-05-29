update event_handler
set event_name = 'Prosecution witness absent: police'
where event_type = '20198' and event_sub_type = '13916';

update event_handler
set event_name = 'Prosecution witness absent: professional / expert'
where event_type = '20198' and event_sub_type = '13917';

update event_handler
set event_name = 'Prosecution witness absent: other'
where event_type = '20198' and event_sub_type = '13918';

update event_handler
set event_name = 'Defence increased time estimate - insufficient time for trial to start'
where event_type = '20198' and event_sub_type = '13930';

update event_handler
set event_name = 'Equipment / accommodation failure'
where event_type = '20198' and event_sub_type = '13937';

update event_handler
set event_name = 'Defendant subject to be electronically monitored curfew'
where event_type = '20935' and event_sub_type = '10633';

update event_handler
set event_name = 'Defendant subject to be electronically monitored curfew'
where event_type = '20936' and event_sub_type = '10633';

update event_handler
set event_name = 'Defendant subject to be electronically monitored curfew'
where event_type = '21500' and event_sub_type = '13703';

update event_handler
set handler  = 'StandardEventHandler'
where event_type = '2198' and event_sub_type = '3934';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '3010' and event_sub_type  is NULL;

update event_handler
set handler  = 'StopAndCloseHandler'
where event_type = '30300' and event_sub_type  is NULL;

update event_handler
set handler  = 'DarStopHandler'
where event_type = '30500' and event_sub_type  is NULL;

update event_handler
set handler  = 'DarStopHandler'
where event_type = '30600' and event_sub_type  is NULL;

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40750' and event_sub_type = '11533';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40750' and event_sub_type = '11534';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40750' and event_sub_type = '13507';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40750' and event_sub_type = '13508';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40751' and event_sub_type = '11533';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40751' and event_sub_type = '11534';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40751' and event_sub_type = '13507';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40751' and event_sub_type = '13508';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40752' and event_sub_type = '11533';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40752' and event_sub_type = '11534';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40752' and event_sub_type = '13507';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40752' and event_sub_type = '13508';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40753' and event_sub_type = '11533';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40753' and event_sub_type = '11534';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40753' and event_sub_type = '13507';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40753' and event_sub_type = '13508';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40754' and event_sub_type = '11533';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40754' and event_sub_type = '11534';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40754' and event_sub_type = '13507';

update event_handler
set handler  = 'SentencingRemarksAndRetentionPolicyHandler'
where event_type = '40754' and event_sub_type = '13508';

ALTER SEQUENCE evh_seq RESTART WITH 600;

insert into event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, created_ts, is_reporting_restriction)
    (select nextval('evh_seq'), '20937', '10624', '<Sentence remarks filmed>', 'StandardEventHandler', true, current_timestamp, false);

insert into event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, created_ts, is_reporting_restriction)
    (select nextval('evh_seq'), '20937', '10625', '<Sentence remarks not filmed>', 'StandardEventHandler', true, current_timestamp, false);

update event_handler
set event_name = 'Disqualification Order (from working with children) – ADULTS',
    handler  = 'StandardEventHandler'
where event_type = '40750' and event_sub_type = '12400';

update event_handler
set event_name = 'Disqualification Order (from working with children) – JUVENILES',
    handler  = 'StandardEventHandler'
where event_type = '40750' and event_sub_type = '12401';

update event_handler
set event_name = 'Disqualification Order (from working with children) – ADULTS',
    handler  = 'StandardEventHandler'
where event_type = '40751' and event_sub_type = '12400';

update event_handler
set event_name = 'Disqualification Order (from working with children) – JUVENILES',
    handler  = 'StandardEventHandler'
where event_type = '40751' and event_sub_type = '12401';

update event_handler
set event_name = 'Disqualification Order (from working with children) – ADULTS',
    handler  = 'StandardEventHandler'
where event_type = '40752' and event_sub_type = '12400';

update event_handler
set event_name = 'Disqualification Order (from working with children) – JUVENILES',
    handler  = 'StandardEventHandler'
where event_type = '40752' and event_sub_type = '12401';

update event_handler
set event_name = 'Disqualification Order (from working with children) – ADULTS',
    handler  = 'StandardEventHandler'
where event_type = '40753' and event_sub_type = '12400';

update event_handler
set event_name = 'Disqualification Order (from working with children) – JUVENILES',
    handler  = 'StandardEventHandler'
where event_type = '40753' and event_sub_type = '12401';

delete from hearing_event_ae he
where he.eve_id in (
    select eve_id from event e
    inner join event_handler eh on eh.evh_id = e.evh_id
    where eh.evh_id in (
        select evh_id from event_handler
        where event_type = '40730' and event_sub_type = '10808'
            or event_type = '40731' and event_sub_type = '10808'
            or event_type = '40732' and event_sub_type = '10808'
            or event_type = '40733' and event_sub_type = '10808'
            or event_type = '40734' and event_sub_type = '10808'
            or event_type = '40735' and event_sub_type is null
            or event_type = 'DETTO' and event_sub_type = '11531'
            or event_type = 'STS' and event_sub_type = '11530'
            or event_type = 'STS1821' and event_sub_type = '11532'));

delete from event e
where e.evh_id in (
    select evh_id from event_handler
    where event_type = '40730' and event_sub_type = '10808'
        or event_type = '40731' and event_sub_type = '10808'
        or event_type = '40732' and event_sub_type = '10808'
        or event_type = '40733' and event_sub_type = '10808'
        or event_type = '40734' and event_sub_type = '10808'
        or event_type = '40735' and event_sub_type is null
        or event_type = 'DETTO' and event_sub_type = '11531'
        or event_type = 'STS' and event_sub_type = '11530'
        or event_type = 'STS1821' and event_sub_type = '11532');

delete from event_handler
where event_type = '40730' and event_sub_type = '10808'
   or event_type = '40731' and event_sub_type = '10808'
   or event_type = '40732' and event_sub_type = '10808'
   or event_type = '40733' and event_sub_type = '10808'
   or event_type = '40734' and event_sub_type = '10808'
   or event_type = '40735' and event_sub_type is null
   or event_type = 'DETTO' and event_sub_type = '11531'
   or event_type = 'STS' and event_sub_type = '11530'
   or event_type = 'STS1821' and event_sub_type = '11532';

update event_handler
set handler = 'DartsEventNullHandler'
where (event_type = 'CPPDL' and event_sub_type = 'CPPDL')
    or (event_type = 'DL' and event_sub_type is null)
    or (event_type = 'NEWCASE' and event_sub_type is null);