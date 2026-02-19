INSERT INTO retention_confidence_category_mapper (rcc_id, confidence_category, ret_conf_score, ret_conf_reason, description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES
    (nextval('rcc_seq'),999,0,'UNKNOWN','UNRECOGNISED', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
