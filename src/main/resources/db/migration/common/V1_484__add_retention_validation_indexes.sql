--
-- flyway indexes for retention validation
--

CREATE INDEX IF NOT EXISTS wca_cdt_idx ON wk_case_activity_data(closed_date_type);
