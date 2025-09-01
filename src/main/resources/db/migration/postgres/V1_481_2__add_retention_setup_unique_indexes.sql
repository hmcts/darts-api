CREATE UNIQUE INDEX IF NOT EXISTS cc_dets_pk ON cc_dets(cas_id) TABLESPACE pg_default;
ALTER TABLE IF EXISTS cc_dets ADD PRIMARY KEY USING INDEX cc_dets_pk;

CREATE UNIQUE INDEX IF NOT EXISTS cmr_dets_pk ON cmr_dets(cmd_id) TABLESPACE pg_default;
ALTER TABLE IF EXISTS cmr_dets ADD PRIMARY KEY USING INDEX cmr_dets_pk;

CREATE UNIQUE INDEX IF NOT EXISTS cr_dets_pk ON cr_dets(crd_id) TABLESPACE pg_default;
ALTER TABLE IF EXISTS cr_dets ADD PRIMARY KEY USING INDEX cr_dets_pk;

CREATE UNIQUE INDEX IF NOT EXISTS wk_case_correction_pk ON wk_case_correction(cas_id) TABLESPACE pg_default;
ALTER TABLE IF EXISTS wk_case_correction ADD PRIMARY KEY USING INDEX wk_case_correction_pk;
