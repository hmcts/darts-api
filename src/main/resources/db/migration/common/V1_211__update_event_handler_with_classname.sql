UPDATE darts.event_handler
SET handler          = 'DartsEventNullHandler',
    last_modified_ts = current_timestamp,
    last_modified_by = 0
where evh_id = 356
