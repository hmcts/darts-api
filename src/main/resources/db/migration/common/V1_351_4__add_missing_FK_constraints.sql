update external_service_auth_token theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE external_service_auth_token ADD CONSTRAINT external_service_auth_token_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE external_service_auth_token ALTER COLUMN created_by SET NOT NULL;

update external_service_auth_token theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE external_service_auth_token ADD CONSTRAINT external_service_auth_token_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE external_service_auth_token ALTER COLUMN last_modified_by SET NOT NULL;


update hearing theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE hearing ADD CONSTRAINT hearing_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE hearing ALTER COLUMN created_by SET NOT NULL;

update hearing theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE hearing ADD CONSTRAINT hearing_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE hearing ALTER COLUMN last_modified_by SET NOT NULL;

update hearing set created_ts = current_timestamp where created_ts is null;
ALTER TABLE hearing ALTER COLUMN created_ts SET NOT NULL;

update hearing set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE hearing ALTER COLUMN last_modified_ts SET NOT NULL;

update judge theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE judge ADD CONSTRAINT judge_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE judge ALTER COLUMN created_by SET NOT NULL;

update judge theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE judge ADD CONSTRAINT judge_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE judge ALTER COLUMN last_modified_by SET NOT NULL;

update judge set created_ts = current_timestamp where created_ts is null;
ALTER TABLE judge ALTER COLUMN created_ts SET NOT NULL;

update judge set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE judge ALTER COLUMN last_modified_ts SET NOT NULL;


update media theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE media ADD CONSTRAINT media_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE media ALTER COLUMN created_by SET NOT NULL;

update media theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE media ADD CONSTRAINT media_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE media ALTER COLUMN last_modified_by SET NOT NULL;

update media set created_ts = current_timestamp where created_ts is null;
ALTER TABLE media ALTER COLUMN created_ts SET NOT NULL;

update media set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE media ALTER COLUMN last_modified_ts SET NOT NULL;


ALTER TABLE media_request ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN last_modified_by SET NOT NULL;


