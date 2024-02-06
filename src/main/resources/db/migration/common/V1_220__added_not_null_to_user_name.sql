-- v49
UPDATE user_account
SET user_name = 'system'
WHERE user_name IS NULL;
ALTER TABLE user_account
  ALTER COLUMN user_name SET NOT NULL;
