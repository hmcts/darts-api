CREATE INDEX ctr_cn_trgm_idx ON courtroom USING gin (courtroom_name gin_trgm_ops);
