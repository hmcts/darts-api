UPDATE courtroom theTable SET created_by = 0 WHERE created_by IS NULL OR NOT exists (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.created_by);
ALTER TABLE courtroom ADD CONSTRAINT courtroom_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

UPDATE courtroom SET created_ts = current_timestamp WHERE created_ts IS NULL;
ALTER TABLE courtroom ALTER COLUMN created_ts SET NOT NULL;
