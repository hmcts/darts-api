CREATE INDEX IF NOT EXISTS cc_rr_idx
    ON darts.court_case (retention_retries)
    WHERE is_retention_updated = true;
