
CREATE TABLE moj_case
(moj_cas_id                INTEGER      NOT NULL
,moj_rer_id                INTEGER
,r_case_object_id          CHARACTER VARYING(16)
,c_type                    CHARACTER VARYING
,c_case_id                 CHARACTER VARYING
,c_closed                  BOOLEAN
,c_interpreter_used        BOOLEAN
,c_case_closed_ts          TIMESTAMP WITH TIME ZONE
,c_defendant               CHARACTER VARYING[]
,c_prosecutor              CHARACTER VARYING[]
,c_defence                 CHARACTER VARYING[]
,retain_until_ts           TIMESTAMP WITH TIME ZONE
,r_version_label           CHARACTER VARYING(32)
,i_superseded              BOOLEAN
,i_version                 SMALLINT);


ALTER TABLE moj_case                      ADD PRIMARY KEY (moj_cas_id);

ALTER TABLE moj_case                      ADD CONSTRAINT moj_case_reporting_restriction_fk
FOREIGN KEY (moj_rer_id) REFERENCES moj_reporting_restrictions(moj_rer_id);

CREATE SEQUENCE IF NOT EXISTS moj_cas_seq;
