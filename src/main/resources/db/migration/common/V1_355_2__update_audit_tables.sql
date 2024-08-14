update audit theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE audit ADD CONSTRAINT audit_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE audit ALTER COLUMN created_by SET NOT NULL;

update audit theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE audit ADD CONSTRAINT audit_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE audit ALTER COLUMN last_modified_by SET NOT NULL;

ALTER TABLE audit ALTER COLUMN last_modified_ts SET NOT NULL;

update audit_activity theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE audit_activity ADD CONSTRAINT audit_activity_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

update audit_activity theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE audit_activity ADD CONSTRAINT audit_activity_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
