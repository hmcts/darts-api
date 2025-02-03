ALTER TABLE rps_retainer
    ADD CONSTRAINT rps_retainer_pk PRIMARY KEY (rpr_id);

ALTER TABLE case_retention_extra
    ADD CONSTRAINT case_retention_extra_pk PRIMARY KEY (cas_id);

ALTER TABLE case_retention_audit_heritage
    ADD CONSTRAINT case_retention_audit_heritage_pk PRIMARY KEY (rah_id);

ALTER TABLE retention_policy_type_heritage_mapping
    ADD CONSTRAINT retention_policy_type_heritage_mapping_pk PRIMARY KEY (rhm_id);
