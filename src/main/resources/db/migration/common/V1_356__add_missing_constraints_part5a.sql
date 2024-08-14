update node_register theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE node_register ALTER COLUMN created_by SET NOT NULL;
update node_register set created_ts = current_timestamp where created_ts is null;
ALTER TABLE node_register ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE node_register ALTER COLUMN node_type SET NOT NULL;

update object_retrieval_queue theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE object_retrieval_queue ADD CONSTRAINT object_retrieval_queue_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

update object_retrieval_queue theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE object_retrieval_queue ADD CONSTRAINT object_retrieval_queue_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

update report theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE report ADD CONSTRAINT report_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE report ALTER COLUMN created_by SET NOT NULL;

update report theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE report ADD CONSTRAINT report_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE report ALTER COLUMN last_modified_by SET NOT NULL;

update report set created_ts = current_timestamp where created_ts is null;
ALTER TABLE report ALTER COLUMN created_ts SET NOT NULL;

update report set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE report ALTER COLUMN last_modified_ts SET NOT NULL;

