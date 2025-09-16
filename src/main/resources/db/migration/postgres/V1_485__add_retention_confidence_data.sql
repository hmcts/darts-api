DROP TABLE IF EXISTS wk_case_confidence_level;

CREATE TABLE IF NOT EXISTS wk_case_confidence_level
(
    cas_id                         INTEGER  NOT NULL,
    confidence_level               INTEGER
);

CREATE UNIQUE INDEX IF NOT EXISTS  wk_case_confidence_level_pk ON wk_case_confidence_level(cas_id);

ALTER TABLE IF EXISTS wk_case_confidence_level ADD PRIMARY KEY USING INDEX wk_case_confidence_level_pk;