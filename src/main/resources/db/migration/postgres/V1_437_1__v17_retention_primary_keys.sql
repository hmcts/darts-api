CREATE UNIQUE INDEX rps_retainer_pk ON rps_retainer (rpr_id);
ALTER TABLE rps_retainer
    ADD PRIMARY KEY USING INDEX rps_retainer_pk;

CREATE UNIQUE INDEX case_retention_extra_pk ON case_retention_extra (cas_id);
ALTER TABLE case_retention_extra
    ADD PRIMARY KEY USING INDEX case_retention_extra_pk;

CREATE UNIQUE INDEX case_retention_audit_heritage_pk ON case_retention_audit_heritage (rah_id);
ALTER TABLE case_retention_audit_heritage
    ADD PRIMARY KEY USING INDEX case_retention_audit_heritage_pk;

CREATE UNIQUE INDEX retention_policy_type_heritage_mapping_pk ON retention_policy_type_heritage_mapping (rhm_id);
ALTER TABLE retention_policy_type_heritage_mapping
    ADD PRIMARY KEY USING INDEX retention_policy_type_heritage_mapping_pk;
