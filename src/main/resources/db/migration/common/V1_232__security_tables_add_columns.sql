ALTER TABLE security_group
  ADD COLUMN global_access boolean NOT NULL DEFAULT FALSE;

ALTER TABLE security_group
  ADD COLUMN display_state boolean NOT NULL DEFAULT TRUE;

ALTER TABLE security_role ADD COLUMN display_name CHARACTER VARYING;
UPDATE security_role SET display_name = '';
ALTER TABLE security_role ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE security_role ADD COLUMN display_state boolean NOT NULL DEFAULT TRUE;

ALTER TABLE user_account ADD COLUMN account_guid CHARACTER VARYING;
ALTER TABLE user_account ADD COLUMN is_system_user boolean NOT NULL DEFAULT FALSE;
