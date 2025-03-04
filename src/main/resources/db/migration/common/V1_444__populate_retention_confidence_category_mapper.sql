INSERT INTO retention_confidence_category_mapper (rcc_id, confidence_category, ret_conf_score, ret_conf_reason, description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES
    (1, 1, 0, 'CASE_CLOSED', 'Case Close Event', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (2, 2, 0, 'MANUAL_OVERRIDE', 'Manual Override', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (3, 3, 1, 'AGED_CASE', 'Aged Case', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (4, 21, 0, 'MANUAL_OVERRIDE', 'Manual Overrides - MANUAL and CUSTODIAL. Must be closed using case closed event_ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (5, 22, 0, 'MANUAL_OVERRIDE', 'Manual Overrides - PERMANENT. Must be closed using case closed event_ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (6, 23, 0, 'CASE_CLOSED', 'Variable Retention - COMPLETED. Must be closed using case closed event_ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (7, 42, 0, 'CASE_CLOSED', 'Variable Retention - READY outside of grace period. Must be closed using case closed event_ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (8, 43, 0, 'CASE_CLOSED', 'RCC not set by migration', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 0),
    (9, 31, 0, 'CASE_CLOSED', 'Retainers Created Pre Variable Retention. Closed using case closed event_ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (10, 32, 1, 'MAX_EVENT_CLOSED', 'Retainers Created Pre Variable Retention. Closed using max event ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (11, 33, 1, 'MAX_MEDIA_CLOSED', 'Retainers Created Pre Variable Retention. Closed using max media ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (12, 34, 1, 'CASE_CREATION_CLOSED', 'Retainers Created Pre Variable Retention. Closed using case creation ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (13, 11, 0, 'CASE_CLOSED', 'Catch-up Clean-up. Closed using case closed event_ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (14, 12, 1, 'MAX_EVENT_CLOSED', 'Catch-up Clean-up. Closed using max event ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (15, 13, 1, 'MAX_MEDIA_CLOSED', 'Catch-up Clean-up. Closed using max media ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (16, 14, 1, 'CASE_CREATION_CLOSED', 'Catch-up Clean-up. Closed using case creation ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (17, 61, 0, 'CASE_CLOSED', 'Aged Cases. Closed using case closed event_ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (18, 62, 1, 'MAX_EVENT_CLOSED', 'Aged Cases. Closed using max event ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (19, 63, 1, 'MAX_MEDIA_CLOSED', 'Aged Cases. Closed using max media ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0),
    (20, 64, 1, 'CASE_CREATION_CLOSED', 'Aged Cases. Closed using case creation ts', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

ALTER SEQUENCE rcc_seq RESTART WITH 100;
