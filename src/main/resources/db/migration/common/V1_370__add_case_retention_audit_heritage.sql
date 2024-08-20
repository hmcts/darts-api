CREATE TABLE case_retention_audit_heritage
(r_object_id                 CHARACTER VARYING(16)         NOT NULL
,i_partition                 INTEGER
,c_case_id                   CHARACTER VARYING(32)
,c_date_retention_amended    TIMESTAMP WITH TIME ZONE
,c_comments                  CHARACTER VARYING
,c_date_previous_retention   TIMESTAMP WITH TIME ZONE
,c_username                  CHARACTER VARYING(32)
,c_status                    CHARACTER VARYING(32)
,c_courthouse                CHARACTER VARYING(64)
,c_policy_type               CHARACTER VARYING(20)
,c_case_closed_date          TIMESTAMP WITH TIME ZONE
);