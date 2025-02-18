CREATE INDEX dfd_dn_trgm_idx ON defendant USING gin (defendant_name gin_trgm_ops);
CREATE INDEX eve_evt_trgm_idx ON event USING gin (event_text gin_trgm_ops);
CREATE INDEX jud_jn_trgm_idx ON judge USING gin (judge_name gin_trgm_ops);
CREATE INDEX cas_cn_trgm_idx ON court_case USING gin (case_number gin_trgm_ops);
CREATE INDEX ctr_cn_trgm_idx ON courtroom USING gin (courtroom_name gin_trgm_ops);
