
UPDATE security_group SET created_ts = CURRENT_TIMESTAMP WHERE created_ts IS NULL;
ALTER TABLE security_group ALTER COLUMN created_ts SET NOT NULL;

UPDATE security_group theTable SET created_by = 0 WHERE created_by  IS NULL OR NOT EXISTS (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.created_by);
ALTER TABLE security_group ADD CONSTRAINT security_group_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

UPDATE security_group SET last_modified_ts = CURRENT_TIMESTAMP WHERE last_modified_ts IS NULL;
ALTER TABLE security_group ALTER COLUMN last_modified_ts SET NOT NULL;

UPDATE security_group theTable SET last_modified_by = 0 WHERE last_modified_by  IS NULL OR NOT EXISTS (SELECT 1 FROM user_account ua WHERE ua.usr_id = theTable.last_modified_by);
ALTER TABLE security_group ADD CONSTRAINT security_group_last_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

UPDATE security_group SET display_name = group_name WHERE display_name IS NULL;
ALTER TABLE security_group ALTER COLUMN display_name SET NOT NULL;
