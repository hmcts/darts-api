UPDATE daily_list theTable SET created_by = 0 WHERE created_by IS NULL OR NOT exists (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.created_by);
ALTER TABLE daily_list ADD CONSTRAINT daily_list_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE daily_list ALTER COLUMN created_by SET NOT NULL;

UPDATE daily_list theTable SET last_modified_by = 0 WHERE last_modified_by IS NULL OR NOT exists (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.last_modified_by);
ALTER TABLE daily_list ADD CONSTRAINT daily_list_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE daily_list ALTER COLUMN last_modified_by SET NOT NULL;

UPDATE daily_list SET created_ts = current_timestamp WHERE created_ts IS NULL;
ALTER TABLE daily_list ALTER COLUMN created_ts SET NOT NULL;

UPDATE daily_list SET last_modified_ts = current_timestamp WHERE last_modified_ts IS NULL;
ALTER TABLE daily_list ALTER COLUMN last_modified_ts SET NOT NULL;
