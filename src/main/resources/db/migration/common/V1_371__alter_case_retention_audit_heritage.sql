ALTER TABLE case_retention_audit_heritage ADD COLUMN object_name                 CHARACTER VARYING(255);
ALTER TABLE case_retention_audit_heritage ADD COLUMN r_creator_name              CHARACTER VARYING(32);
ALTER TABLE case_retention_audit_heritage ADD COLUMN r_creation_date             TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_retention_audit_heritage ADD COLUMN r_modifier                  CHARACTER VARYING(32);
ALTER TABLE case_retention_audit_heritage ADD COLUMN r_modify_date               TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_retention_audit_heritage ADD COLUMN owner_name                  CHARACTER VARYING(32);