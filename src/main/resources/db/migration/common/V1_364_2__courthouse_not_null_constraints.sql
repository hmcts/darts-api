UPDATE courthouse theTable SET created_by = 0 WHERE created_by IS NULL OR NOT exists (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.created_by);
ALTER TABLE courthouse ADD CONSTRAINT courthouse_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE courthouse ALTER COLUMN created_by SET NOT NULL;

UPDATE courthouse theTable SET last_modified_by = 0 WHERE last_modified_by IS NULL OR NOT exists (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.last_modified_by);
ALTER TABLE courthouse ADD CONSTRAINT courthouse_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE courthouse ALTER COLUMN last_modified_by SET NOT NULL;
