update event_handler
set handler = 'DartsEventNullHandler'
where (event_type = 'UPDCASE' and event_sub_type is null);