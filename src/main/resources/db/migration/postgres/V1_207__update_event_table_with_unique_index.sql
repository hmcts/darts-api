CREATE UNIQUE INDEX event_handler_event_type_event_event_sub_type_unq ON darts.event_handler (event_type, event_sub_type) where active;
