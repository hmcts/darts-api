update defence theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE defence ADD CONSTRAINT defence_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE defence ALTER COLUMN created_by SET NOT NULL;

update defence theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE defence ADD CONSTRAINT defence_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE defence ALTER COLUMN last_modified_by SET NOT NULL;

update defence set created_ts = current_timestamp where created_ts is null;
ALTER TABLE defence ALTER COLUMN created_ts SET NOT NULL;

update defence set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE defence ALTER COLUMN last_modified_ts SET NOT NULL;


update defendant theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE defendant ADD CONSTRAINT defendant_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE defendant ALTER COLUMN created_by SET NOT NULL;

update defendant theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE defendant ADD CONSTRAINT defendant_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE defendant ALTER COLUMN last_modified_by SET NOT NULL;


update event theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE event ADD CONSTRAINT event_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE event ALTER COLUMN created_by SET NOT NULL;

update event theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE event ADD CONSTRAINT event_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE event ALTER COLUMN last_modified_by SET NOT NULL;

update event set created_ts = current_timestamp where created_ts is null;
ALTER TABLE event ALTER COLUMN created_ts SET NOT NULL;

update event set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE event ALTER COLUMN last_modified_ts SET NOT NULL;

ALTER TABLE event ALTER COLUMN event_status TYPE INTEGER USING event_status::integer;


update event_handler theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE event_handler ADD CONSTRAINT event_handler_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE event_handler ALTER COLUMN created_by SET NOT NULL;


