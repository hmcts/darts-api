ALTER TABLE audit
  DROP CONSTRAINT audit_pkey;

ALTER TABLE audit
  ADD CONSTRAINT audit_pk PRIMARY KEY (aud_id);

ALTER TABLE audit_activity
  rename CONSTRAINT audit_activities_pkey to audit_activity_pk;

ALTER TABLE external_service_auth_token
  ADD CONSTRAINT external_service_auth_token_pk PRIMARY KEY (esa_id);

ALTER TABLE transcription_urgency
  ADD CONSTRAINT transcription_urgency_pk PRIMARY KEY (tru_id);



ALTER TABLE external_service_auth_token
  ADD CONSTRAINT token_type_ck CHECK (token_type in (1, 2));

