CREATE INDEX cas_cn_trgm_idx ON court_case USING gin (case_number gin_trgm_ops);
