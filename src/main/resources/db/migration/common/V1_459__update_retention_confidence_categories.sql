insert into retention_confidence_category_mapper(rcc_id, confidence_category, ret_conf_score, ret_conf_reason, description, created_by, created_ts,
                                                 last_modified_by,
                                                 last_modified_ts)
VALUES (nextval('rcc_seq'), 4, 0, 'CASE_CLOSED', 'Aged Cases. Closed using case closed event_ts', 0, current_timestamp, 0, current_timestamp),
       (nextval('rcc_seq'), 5, 1, 'MAX_EVENT_CLOSED', 'Aged Cases. Closed using max event ts', 0, current_timestamp, 0, current_timestamp),
       (nextval('rcc_seq'), 6, 1, 'MAX_MEDIA_CLOSED', 'Aged Cases. Closed using max media ts', 0, current_timestamp, 0, current_timestamp),
       (nextval('rcc_seq'), 7, 1, 'MAX_HEARING_CLOSED', 'Aged Cases. Closed using max hearing ts', 0, current_timestamp, 0, current_timestamp),
       (nextval('rcc_seq'), 8, 1, 'CASE_CREATION_CLOSED', 'Aged Cases. Closed using case creation ts', 0, current_timestamp, 0, current_timestamp);
