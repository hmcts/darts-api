update user_account set created_ts = current_timestamp where created_ts is null;
ALTER TABLE user_account ALTER COLUMN created_ts SET NOT NULL;

update user_account set created_by = 0 where created_by is null;
ALTER TABLE user_account ALTER COLUMN created_by SET NOT NULL;

update user_account set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE user_account ALTER COLUMN last_modified_ts SET NOT NULL;

update user_account set last_modified_by = 0 where last_modified_by is null;
ALTER TABLE user_account ALTER COLUMN last_modified_by SET NOT NULL;
