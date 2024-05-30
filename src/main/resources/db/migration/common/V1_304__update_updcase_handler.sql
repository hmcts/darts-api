update event_handler
set handler = 'DartsEventNullHandler'
where (event_type = 'UPDCASE' and event_sub_type is null);

update event_handler
set event_name = 'Equipment / accommodation'
where event_type = '20198' and event_sub_type = '13937';

update event_handler
set event_name = 'Appellant Read'
where event_type = '20935' and event_sub_type = '10633';

update event_handler
set event_name = 'Appellant Read'
where event_type = '20936' and event_sub_type = '10633';

update event_handler
set event_name = 'Defendant subject to an electronically monitored curfew'
where event_type = '21500' and event_sub_type = '13703';
