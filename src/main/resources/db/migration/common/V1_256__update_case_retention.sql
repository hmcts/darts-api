--Retention
ALTER TABLE case_retention
  DROP COLUMN submitted_ts;
ALTER TABLE case_retention
  ADD COLUMN submitted_by INTEGER NOT NULL;
ALTER TABLE case_retention
  ADD COLUMN last_modified_ts TIMESTAMP WITH TIME ZONE NOT NULL;
ALTER TABLE case_retention
  ADD COLUMN last_modified_by INTEGER NOT NULL;

ALTER TABLE case_retention
  ADD CONSTRAINT case_retention_created_by_fk
    FOREIGN KEY (created_by) REFERENCES user_account (usr_id);

ALTER TABLE case_retention
  ADD CONSTRAINT case_retention_last_modified_by_fk
    FOREIGN KEY (last_modified_by) REFERENCES user_account (usr_id);

ALTER TABLE case_retention
  ADD CONSTRAINT case_retention_submitted_by_fk
    FOREIGN KEY (submitted_by) REFERENCES user_account (usr_id);

--Security
ALTER TABLE security_group
  DROP COLUMN r_dm_group_s_object_id;
ALTER TABLE security_group
  DROP COLUMN r_modify_date;
ALTER TABLE security_group
  ADD COLUMN display_name CHARACTER VARYING;
ALTER TABLE security_group
  ADD COLUMN dm_group_s_object_id CHARACTER VARYING(16);
COMMENT ON COLUMN security_group.dm_group_s_object_id
  IS 'internal Documentum primary key from dm_group_s';

