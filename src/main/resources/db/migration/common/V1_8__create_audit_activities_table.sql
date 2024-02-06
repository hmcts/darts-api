CREATE TABLE IF NOT EXISTS darts.audit_activities
(
  id          INTEGER           NOT NULL,
  name        CHARACTER VARYING NOT NULL,
  description CHARACTER VARYING NOT NULL,
  CONSTRAINT audit_activities_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS audit_activities_seq;

ALTER TABLE darts.audit
  ADD FOREIGN KEY (audit_activity_id) REFERENCES darts.audit_activities (id);

