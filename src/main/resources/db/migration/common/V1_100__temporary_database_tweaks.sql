ALTER TABLE darts.event_handler DROP COLUMN last_modified_by;
ALTER TABLE darts.event ALTER COLUMN event_id TYPE INTEGER;
ALTER TABLE darts.user_account ALTER COLUMN user_state TYPE INTEGER;

CREATE TABLE darts.prosecutors
(pro_id                     INTEGER							 NOT NULL
,cas_id                     INTEGER							 NOT NULL
,prosecutor_name            CHARACTER VARYING							 NOT NULL
);
ALTER TABLE darts.court_case DROP COLUMN prosecutor_list;
CREATE SEQUENCE darts.pro_seq CACHE 20;

CREATE TABLE darts.defence
(dfc_id                     INTEGER							 NOT NULL
,cas_id                     INTEGER							 NOT NULL
,defence_name            CHARACTER VARYING							 NOT NULL
);
ALTER TABLE darts.court_case DROP COLUMN defence_list;
CREATE SEQUENCE darts.dfc_seq CACHE 20;

CREATE TABLE darts.defendants
(dfd_id                     INTEGER							 NOT NULL
,cas_id                     INTEGER							 NOT NULL
,defendant_name            CHARACTER VARYING							 NOT NULL
);
ALTER TABLE darts.court_case DROP COLUMN defendant_list;
CREATE SEQUENCE darts.dfd_seq CACHE 20;

CREATE TABLE darts.judges
(jud_id                     INTEGER							 NOT NULL
,hea_id                     INTEGER							 NOT NULL
,judge_name            CHARACTER VARYING							 NOT NULL
);
ALTER TABLE darts.hearing DROP COLUMN judge_list;
CREATE SEQUENCE darts.jud_seq CACHE 20;
