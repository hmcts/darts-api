update court_case theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE court_case ADD CONSTRAINT court_case_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE court_case ALTER COLUMN created_by SET NOT NULL;

update court_case theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE court_case ADD CONSTRAINT court_case_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE court_case ALTER COLUMN last_modified_by SET NOT NULL;

update court_case set created_ts = current_timestamp where created_ts is null;
ALTER TABLE court_case ALTER COLUMN created_ts SET NOT NULL;

update court_case set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE court_case ALTER COLUMN last_modified_ts SET NOT NULL;

update courthouse theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE courthouse ADD CONSTRAINT courthouse_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE courthouse ALTER COLUMN created_by SET NOT NULL;

update courthouse theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE courthouse ADD CONSTRAINT courthouse_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE courthouse ALTER COLUMN last_modified_by SET NOT NULL;


update courtroom theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE courtroom ADD CONSTRAINT courtroom_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

update courtroom set created_ts = current_timestamp where created_ts is null;
ALTER TABLE courtroom ALTER COLUMN created_ts SET NOT NULL;


update daily_list theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE daily_list ADD CONSTRAINT daily_list_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE daily_list ALTER COLUMN created_by SET NOT NULL;

update daily_list theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE daily_list ADD CONSTRAINT daily_list_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE daily_list ALTER COLUMN last_modified_by SET NOT NULL;

update daily_list set created_ts = current_timestamp where created_ts is null;
ALTER TABLE daily_list ALTER COLUMN created_ts SET NOT NULL;

update daily_list set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE daily_list ALTER COLUMN last_modified_ts SET NOT NULL;

