ALTER TABLE user_account
  ADD COLUMN user_full_name CHARACTER VARYING;
update darts.user_account
set user_full_name = user_name;

ALTER TABLE user_account
  ALTER COLUMN user_full_name SET NOT NULL;

