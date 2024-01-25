-- v3 initial version, as retention specific script
-- v4 rename case_retention_audit to case_retention_audit
--    add created* and last_modified* to case_retention
--    remove manual_override_retention
--    add comments and submitted_ts to case_retention
--    remove last* from case_retention
--    add legacy object_id fields to case_retention and retention_policy_type
--    change retention field total_sentence from integer to character varying
--    change sentence_name to policy_name on retention_policy_type
-- v5 add is_manual_override to case_retention
--    add event_ts to case_management_retention
--    amend eve_id on case_managment_retention to be nullable
-- v6 change submitted_ts to submitted_by on case_retention and FK for same, and missing one on created_by
--    add last_modified* to case_retention and FK 
-- v7 change retention_policy_type.duration from integer to character varying, to support nYnMnD format used elsewhere
--    amend retention_policy_type.policy_end_ts to nullable
--    amend total_sentence on case_management_retention & case_retention to nullable
--    add display_name and description to retention_policy_type
--    remove is_manual_override  from case_retention
--    amend fixed_policy_key from integer to character varying

SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;

--List of Table Aliases
-- case_management_retention         CMR
-- case_retention                    CAR   
-- retention_policy_type             RPT


CREATE TABLE case_management_retention
(cmr_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,rpt_id                      INTEGER                       NOT NULL
,eve_id                      INTEGER                       
,event_ts                    TIMESTAMP WITH TIME ZONE      
,total_sentence              CHARACTER VARYING                       -- < is this integer or the nYnMnD >
) TABLESPACE darts_tables;

CREATE TABLE case_retention
(car_id                      INTEGER                       NOT NULL
,cas_id                      INTEGER                       NOT NULL
,rpt_id                      INTEGER                       NOT NULL
,cmr_id                      INTEGER            
,total_sentence              CHARACTER VARYING                       -- < is this integer or the nYnMnD >
,retain_until_ts             TIMESTAMP WITH TIME ZONE      NOT NULL 
,retain_until_applied_on_ts  TIMESTAMP WITH TIME ZONE      NOT NULL 
,current_state               CHARACTER VARYING             NOT NULL  -- can we agree on single chars, eg P-pending, E-expired, A-active
,comments                    CHARACTER VARYING 
,retention_object_id         CHARACTER VARYING                       -- PK of legacy source migration table 
,submitted_by                INTEGER                       NOT NULL
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

CREATE TABLE retention_policy_type
(rpt_id                      INTEGER                       NOT NULL
,fixed_policy_key            CHARACTER VARYING             NOT NULL 
,policy_name                 CHARACTER VARYING             NOT NULL
,display_name                CHARACTER VARYING             NOT NULL
,duration                    CHARACTER VARYING             NOT NULL -- changed to accommodate nYnMnD
,policy_start_ts             TIMESTAMP WITH TIME ZONE      NOT NULL
,policy_end_ts               TIMESTAMP WITH TIME ZONE  
,description                 CHARACTER VARYING             NOT NULL  
,retention_policy_object_id  CHARACTER VARYING                      -- PK of legacy source migration table
,created_ts                  TIMESTAMP WITH TIME ZONE      NOT NULL
,created_by                  INTEGER                       NOT NULL
,last_modified_ts            TIMESTAMP WITH TIME ZONE      NOT NULL
,last_modified_by            INTEGER                       NOT NULL
) TABLESPACE darts_tables;

CREATE UNIQUE INDEX case_management_retention_pk ON case_management_retention(cmr_id) TABLESPACE darts_indexes;
ALTER TABLE case_management_retention ADD PRIMARY KEY USING INDEX case_management_retention_pk;

CREATE UNIQUE INDEX case_retention_pk ON case_retention(car_id) TABLESPACE darts_indexes; 
ALTER TABLE case_retention            ADD PRIMARY KEY USING INDEX case_retention_pk;

CREATE UNIQUE INDEX retention_policy_type_pk ON retention_policy_type(rpt_id) TABLESPACE darts_indexes; 
ALTER TABLE retention_policy_type     ADD PRIMARY KEY USING INDEX retention_policy_type_pk;

CREATE SEQUENCE cmr_seq CACHE 20;
CREATE SEQUENCE car_seq CACHE 20;
CREATE SEQUENCE rpt_seq CACHE 20;

ALTER TABLE case_retention            
ADD CONSTRAINT case_retention_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_retention           
ADD CONSTRAINT case_retention_retention_policy_type_fk
FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE case_retention             
ADD CONSTRAINT case_retention_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention             
ADD CONSTRAINT case_retention_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention             
ADD CONSTRAINT case_retention_submitted_by_fk
FOREIGN KEY (submitted_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention            
ADD CONSTRAINT case_retention_case_management_retention_fk
FOREIGN KEY (cmr_id) REFERENCES case_management_retention(cmr_id);

ALTER TABLE case_management_retention            
ADD CONSTRAINT case_management_retention_court_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_management_retention            
ADD CONSTRAINT case_management_retention_retention_policy_type_fk
FOREIGN KEY (rpt_id) REFERENCES retention_policy_type(rpt_id);

ALTER TABLE case_management_retention            
ADD CONSTRAINT case_management_retention_event_fk
FOREIGN KEY (eve_id) REFERENCES event(eve_id);

ALTER TABLE retention_policy_type             
ADD CONSTRAINT retention_policy_type_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE retention_policy_type             
ADD CONSTRAINT retention_policy_type_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);



GRANT SELECT,INSERT,UPDATE,DELETE ON case_management_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON case_retention TO darts_user;
GRANT SELECT,INSERT,UPDATE,DELETE ON retention_policy_type TO darts_user;

GRANT SELECT,UPDATE ON  cmr_seq TO darts_user;
GRANT SELECT,UPDATE ON  car_seq TO darts_user;
GRANT SELECT,UPDATE ON  rpt_seq TO darts_user;
