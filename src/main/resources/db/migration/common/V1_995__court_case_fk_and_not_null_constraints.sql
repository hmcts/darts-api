UPDATE court_case theTable SET created_by = 0 WHERE created_by IS null OR NOT exists (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.created_by);
ALTER TABLE court_case ADD CONSTRAINT court_case_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE court_case ALTER COLUMN created_by SET NOT NULL;

UPDATE court_case theTable SET last_modified_by = 0 WHERE last_modified_by IS null OR NOT exists (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.last_modified_by);
ALTER TABLE court_case ADD CONSTRAINT court_case_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE court_case ALTER COLUMN last_modified_by SET NOT NULL;

UPDATE court_case SET created_ts = current_timestamp WHERE created_ts IS null;
ALTER TABLE court_case ALTER COLUMN created_ts SET NOT NULL;

UPDATE court_case SET last_modified_ts = current_timestamp WHERE last_modified_ts IS null;
ALTER TABLE court_case ALTER COLUMN last_modified_ts SET NOT NULL;
