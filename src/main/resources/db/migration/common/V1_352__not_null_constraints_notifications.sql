update notification set created_ts = current_timestamp where created_ts is null;
alter table notification alter column created_ts set not null;

update notification set created_by = 0 where created_by is null;
alter table notification alter column created_by set not null;

update notification set last_modified_ts = current_timestamp where last_modified_ts is null;
alter table notification alter column last_modified_ts set not null;

update notification set last_modified_by = 0 where last_modified_by is null;
alter table notification alter column last_modified_by set not null;
