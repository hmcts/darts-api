--
-- flyway indexes for retention preparation
--

CREATE INDEX IF NOT EXISTS ccd_ct_idx ON cc_dets(category_type);
CREATE INDEX IF NOT EXISTS wcc_ct_idx ON wk_case_correction(category_type);
