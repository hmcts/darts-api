CREATE UNIQUE INDEX IF NOT EXISTS wk_case_best_values_p1_pk ON wk_case_best_values_p1(cas_id) TABLESPACE pg_default;
ALTER TABLE IF EXISTS wk_case_best_values_p1 ADD PRIMARY KEY USING INDEX wk_case_best_values_p1_pk;

CREATE UNIQUE INDEX IF NOT EXISTS wk_case_best_values_post_p1_pk ON wk_case_best_values_post_p1(cas_id) TABLESPACE pg_default;
ALTER TABLE IF EXISTS wk_case_best_values_post_p1 ADD PRIMARY KEY USING INDEX wk_case_best_values_post_p1_pk;

CREATE UNIQUE INDEX IF NOT EXISTS wk_case_activity_data_pk ON wk_case_activity_data(cas_id,closed_date_type) TABLESPACE pg_default;
ALTER TABLE IF EXISTS wk_case_activity_data ADD PRIMARY KEY USING INDEX wk_case_activity_data_pk;
