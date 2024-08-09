
UPDATE security_group SET created_ts = CURRENT_TIMESTAMP WHERE created_ts IS NULL;
ALTER TABLE security_group ALTER COLUMN created_ts SET NOT NULL;

UPDATE security_group SET created_by = 0 WHERE created_by IS NULL;
ALTER TABLE security_group ALTER COLUMN created_by SET NOT NULL;

UPDATE security_group SET last_modified_ts = CURRENT_TIMESTAMP WHERE last_modified_ts IS NULL;
ALTER TABLE security_group ALTER COLUMN last_modified_ts SET NOT NULL;

UPDATE security_group SET last_modified_by = 0 WHERE last_modified_by IS NULL;
ALTER TABLE security_group ALTER COLUMN last_modified_by SET NOT NULL;

UPDATE security_group SET display_name = group_name WHERE display_name IS NULL;
ALTER TABLE security_group ALTER COLUMN display_name SET NOT NULL;
