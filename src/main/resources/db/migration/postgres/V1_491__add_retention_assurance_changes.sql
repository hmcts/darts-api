ALTER TABLE IF EXISTS wk_case_best_values_p1 ALTER COLUMN subtype varchar(30) NULL;
ALTER TABLE IF EXISTS wk_case_best_values_post_p1 ALTER COLUMN subtype varchar(30) NULL;
ALTER TABLE IF EXISTS wk_case_correction ALTER COLUMN closed_date_subtype varchar(30) NULL;

ALTER TABLE IF EXISTS wk_case_best_values_p1 DROP COLUMN rownum;
ALTER TABLE IF EXISTS wk_case_best_values_post_p1 DROP COLUMN rownum;
