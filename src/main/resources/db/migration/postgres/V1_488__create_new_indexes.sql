CREATE INDEX IF NOT EXISTS cr_rr_idx ON case_retention (retention_retries) WHERE is_retention_updated;
