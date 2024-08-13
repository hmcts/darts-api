update notification set created_ts = current_timestamp where created_ts is null;
alter table notification alter column created_ts set not null;

update notification n set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = n.created_by);;
alter table notification alter column created_by set not null;

update notification set last_modified_ts = current_timestamp where last_modified_ts is null;
alter table notification alter column last_modified_ts set not null;

update notification n set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = n.created_by);;
alter table notification alter column last_modified_by set not null;
