CREATE TABLE IF NOT EXISTS wk_case_best_values_p1
(cas_id                         INTEGER
,eve_id                         BIGINT
,evh_id                         INTEGER
,closed_date_ts                 TIMESTAMP WITH TIME ZONE
,closed_date_type               CHARACTER VARYING(50)
,rownum                         INTEGER
);

CREATE TABLE IF NOT EXISTS wk_case_best_values_post_p1
(cas_id                         INTEGER
,eve_id                         BIGINT
,evh_id                         INTEGER
,closed_date_ts                 TIMESTAMP WITH TIME ZONE
,closed_date_type               CHARACTER VARYING(50)
,rownum                         INTEGER
);

CREATE TABLE IF NOT EXISTS wk_case_activity_data
(cas_id                         INTEGER
,eve_id                         BIGINT
,evh_id                         INTEGER
,closed_date_ts                 TIMESTAMP WITH TIME ZONE
,closed_date_type               CHARACTER VARYING(50)
,rownum                         INTEGER
);

CREATE INDEX IF NOT EXISTS wca_cdt_idx ON wk_case_activity_data(closed_date_type);
