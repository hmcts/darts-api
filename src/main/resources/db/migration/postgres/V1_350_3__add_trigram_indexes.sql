CREATE INDEX jud_jn_trgm_idx ON judge USING gin (judge_name gin_trgm_ops);
