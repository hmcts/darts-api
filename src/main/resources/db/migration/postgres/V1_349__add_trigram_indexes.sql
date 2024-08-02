CREATE EXTENSION IF NOT EXISTS pg_trgm;
DROP INDEX dfd_dn_idx;
CREATE INDEX dfd_dn_idx ON defendant USING gin (defendant_name gin_trgm_ops);

CREATE INDEX eve_evt_idx ON event USING gin (event_text gin_trgm_ops);

DROP INDEX jud_jn_idx;
CREATE INDEX jud_jn_idx ON judge USING gin (judge_name gin_trgm_ops);

DROP INDEX cas_cn_idx;
CREATE INDEX cas_cn_idx ON court_case USING gin (case_number gin_trgm_ops);

DROP INDEX ctr_cn_idx;
CREATE INDEX ctr_cn_idx ON courtroom USING gin (courtroom_name gin_trgm_ops);


ANALYSE VERBOSE;