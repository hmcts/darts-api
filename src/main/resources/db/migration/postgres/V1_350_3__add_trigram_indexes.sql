DROP INDEX jud_jn_idx;
CREATE INDEX jud_jn_idx CONCURRENTLY ON judge USING gin (judge_name gin_trgm_ops);
