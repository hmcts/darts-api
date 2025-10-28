--
-- RETENTION v28,v29,v30
--

CREATE TABLE IF NOT EXISTS wk_cr_dets_aligned
(wcda_id                        INTEGER
,car_id                         INTEGER
,cas_id                         INTEGER
,rpt_id                         INTEGER
,cmd_id                         INTEGER
,total_sentence                 CHARACTER VARYING(32)
,retain_until_ts                TIMESTAMP WITH TIME ZONE
,retain_until_applied_on_ts     TIMESTAMP WITH TIME ZONE
,current_state                  CHARACTER VARYING(32)
,comments                       CHARACTER VARYING(150)
,confidence_category            INTEGER
,retention_object_id            CHARACTER VARYING(16)
,submitted_by                   INTEGER
,created_ts                     TIMESTAMP WITH TIME ZONE
,created_by                     INTEGER
,last_modified_ts               TIMESTAMP WITH TIME ZONE
,last_modified_by               INTEGER
);

CREATE TABLE IF NOT EXISTS retention_process_log
(cas_id                         INTEGER
,cr_row_count                   INTEGER
,cmr_row_count                  INTEGER
,processed_ts                   TIMESTAMP WITH TIME ZONE
,status                         CHARACTER VARYING(10)
,message                        CHARACTER VARYING
);

ALTER TABLE case_management_retention ALTER COLUMN eve_id type bigint;
ALTER TABLE cmr_dets ADD COLUMN cmr_id INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS wk_cr_dets_aligned_pk ON wk_cr_dets_aligned(wcda_id);
ALTER TABLE wk_cr_dets_aligned ADD PRIMARY KEY USING INDEX wk_cr_dets_aligned_pk;

CREATE UNIQUE INDEX IF NOT EXISTS retention_process_log_pk ON retention_process_log(cas_id);
ALTER TABLE retention_process_log ADD PRIMARY KEY USING INDEX retention_process_log_pk;

CREATE INDEX IF NOT EXISTS crd_rpt_idx ON cr_dets(rpt_id);
