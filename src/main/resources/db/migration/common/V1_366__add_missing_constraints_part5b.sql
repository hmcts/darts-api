
update prosecutor theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE prosecutor ADD CONSTRAINT prosecutor_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE prosecutor ALTER COLUMN created_by SET NOT NULL;

update prosecutor theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE prosecutor ADD CONSTRAINT prosecutor_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE prosecutor ALTER COLUMN last_modified_by SET NOT NULL;

update prosecutor set created_ts = current_timestamp where created_ts is null;
ALTER TABLE prosecutor ALTER COLUMN created_ts SET NOT NULL;

update prosecutor set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE prosecutor ALTER COLUMN last_modified_ts SET NOT NULL;

