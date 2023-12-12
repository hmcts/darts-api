ALTER TABLE user_account ADD COLUMN is_active BOOLEAN;
update user_account set is_active = true;
ALTER TABLE user_account ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE user_account DROP COLUMN user_state;
