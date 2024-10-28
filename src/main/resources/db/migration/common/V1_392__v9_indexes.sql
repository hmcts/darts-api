-- v9 add 2 event and 2 media indexes
CREATE INDEX eve_ei_ic_idx ON EVENT (event_id, is_current);
CREATE INDEX eve_ts_idx ON EVENT (event_ts);

CREATE INDEX med_mf_idx ON MEDIA (media_file);
CREATE INDEX med_st_et_idx ON MEDIA (start_ts, end_ts);
