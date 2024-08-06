DROP INDEX cas_cn_idx;
CREATE INDEX CONCURRENTLY cas_cn_idx ON court_case USING gin (case_number gin_trgm_ops);
