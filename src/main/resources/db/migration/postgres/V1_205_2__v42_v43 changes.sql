ALTER TABLE audit
  DROP CONSTRAINT audit_pkey;

CREATE UNIQUE INDEX audit_pk ON audit (aud_id);
ALTER TABLE audit
  ADD PRIMARY KEY USING INDEX audit_pk;

ALTER TABLE audit_activity
  rename CONSTRAINT audit_activities_pkey to audit_activity_pk;

CREATE UNIQUE INDEX external_service_auth_token_pk ON external_service_auth_token (esa_id);
ALTER TABLE external_service_auth_token
  ADD PRIMARY KEY USING INDEX external_service_auth_token_pk;

CREATE UNIQUE INDEX transcription_urgency_pk ON transcription_urgency (tru_id);
ALTER TABLE transcription_urgency
  ADD PRIMARY KEY USING INDEX transcription_urgency_pk;



CREATE UNIQUE INDEX courthouse_name_unique_idx on courthouse (UPPER(courthouse_name));
CREATE UNIQUE INDEX courtroom_name_unique_idx on courtroom (cth_id, UPPER(courtroom_name));


ALTER TABLE external_service_auth_token
  ADD CONSTRAINT token_type_ck CHECK (token_type in (1, 2));
