--
-- RETENTION v24,v25,v26,v27
--
-- [listed alphabetically]
--

-- CASE_RETENTION_EXTRA
DROP TABLE IF EXISTS case_retention_extra;

-- CR_DETS
ALTER TABLE IF EXISTS cr_dets ALTER COLUMN retention_object_id TYPE CHARACTER VARYING(16);

-- WK_CASE_ACTIVITY_DATA
ALTER TABLE IF EXISTS wk_case_activity_data DROP COLUMN rownum;
ALTER TABLE IF EXISTS wk_case_activity_data ALTER COLUMN closed_date_type TYPE CHARACTER VARYING(100);

-- WK_CASE_CONFIDENCE_LEVEL
CREATE TABLE IF NOT EXISTS wk_case_confidence_level
(
	cas_id                         INTEGER         NOT NULL,
	confidence_level               INTEGER
);

CREATE UNIQUE INDEX IF NOT EXISTS wk_case_confidence_level_pk ON wk_case_confidence_level(cas_id);
ALTER TABLE IF EXISTS wk_case_confidence_level ADD PRIMARY KEY USING INDEX  wk_case_confidence_level_pk;

-- WK_CASE_CORRECTION
ALTER TABLE IF EXISTS wk_case_correction ADD COLUMN cmr_eve_id BIGINT;

--WK_CRAH_IS_CURRENT_BY_CREATION
CREATE TABLE IF NOT EXISTS wk_crah_is_current_by_creation
(
cas_id                         INTEGER
,rah_id                         INTEGER
,rpt_id                         INTEGER
,c_policy_type                  CHARACTER VARYING(20)
,c_courthouse                   CHARACTER VARYING(64)
,c_case_id                      CHARACTER VARYING(32)
,r_creation_date                TIMESTAMP WITH TIME ZONE
,r_modify_date                  TIMESTAMP WITH TIME ZONE
,c_status                       CHARACTER VARYING(32)
,c_username                     INTEGER
,case_retention_audit_object_id CHARACTER VARYING(16)
,ready_retain_until_date        TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
);

CREATE UNIQUE INDEX IF NOT EXISTS wk_crah_is_current_by_creation_pk ON wk_crah_is_current_by_creation(cas_id);
ALTER TABLE IF EXISTS wk_crah_is_current_by_creation ADD PRIMARY KEY USING INDEX wk_crah_is_current_by_creation_pk;

-- WK_CRAH_IS_CURRENT_BY_LOGIC
CREATE TABLE IF NOT EXISTS wk_crah_is_current_by_logic
(
cas_id                          INTEGER
,rah_id                         INTEGER
,rpt_id                         INTEGER
,c_policy_type                  CHARACTER VARYING(20)
,c_courthouse                   CHARACTER VARYING(64)
,c_case_id                      CHARACTER VARYING(32)
,r_creation_date                TIMESTAMP WITH TIME ZONE
,c_status                       CHARACTER VARYING(32)
,c_username                     INTEGER
,case_retention_audit_object_id CHARACTER VARYING(16)
,ready_retain_until_date        TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
);

CREATE UNIQUE INDEX IF NOT EXISTS wk_crah_is_current_by_logic_pk ON wk_crah_is_current_by_logic(cas_id);
ALTER TABLE IF EXISTS wk_crah_is_current_by_logic ADD PRIMARY KEY USING INDEX wk_crah_is_current_by_logic_pk;

-- WK_RR_IS_CURRENT_BY_CREATION
CREATE TABLE IF NOT EXISTS wk_rr_is_current_by_creation
(
cas_id                          INTEGER
,rpr_id                         INTEGER
,rpt_id                         INTEGER
,case_object_id                 CHARACTER VARYING(16)
,dm_retainer_root_id            CHARACTER VARYING(16)
,dms_object_name                CHARACTER VARYING(64)
,created_ts                     TIMESTAMP WITH TIME ZONE
,last_modified_ts               TIMESTAMP WITH TIME ZONE
,rps_retainer_object_id         CHARACTER VARYING(16)
,dm_retention_date              TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
);

CREATE UNIQUE INDEX IF NOT EXISTS wk_rr_is_current_by_creation_pk ON wk_rr_is_current_by_creation(cas_id);
ALTER TABLE IF EXISTS wk_rr_is_current_by_creation ADD PRIMARY KEY USING INDEX wk_rr_is_current_by_creation_pk;

-- WK_RR_IS_CURRENT_BY_LOGIC
CREATE TABLE IF NOT EXISTS wk_rr_is_current_by_logic
(cas_id                         INTEGER
,rpr_id                         INTEGER
,rpt_id                         INTEGER
,case_object_id                 CHARACTER VARYING(16)
,dm_retainer_root_id            CHARACTER VARYING(16)
,dms_object_name                CHARACTER VARYING(64)
,created_ts                     TIMESTAMP WITH TIME ZONE
,rps_retainer_object_id         CHARACTER VARYING(16)
,dm_retention_date              TIMESTAMP WITH TIME ZONE
,is_current                     BOOLEAN
);

CREATE UNIQUE INDEX IF NOT EXISTS wk_rr_is_current_by_logic_pk ON wk_rr_is_current_by_logic(cas_id);
ALTER TABLE IF EXISTS wk_rr_is_current_by_logic ADD PRIMARY KEY USING INDEX wk_rr_is_current_by_logic_pk;
