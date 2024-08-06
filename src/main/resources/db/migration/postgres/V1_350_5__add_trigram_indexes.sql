DROP INDEX ctr_cn_idx;
CREATE INDEX ctr_cn_idx ON courtroom USING gin (courtroom_name gin_trgm_ops);
