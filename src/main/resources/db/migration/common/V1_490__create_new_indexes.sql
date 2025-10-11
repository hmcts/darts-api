CREATE INDEX IF NOT EXISTS court_case_retention_updated_and_retries_idx ON darts.court_case (is_retention_updated, retention_retries);
