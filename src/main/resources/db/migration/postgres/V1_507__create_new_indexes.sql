CREATE INDEX IF NOT EXISTS cc_rr_idx ON court_case (retention_retries) WHERE is_retention_updated = true;
