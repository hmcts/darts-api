SET search_path TO darts;
ALTER TABLE darts.courthouse DROP CONSTRAINT courthouse_courthouse_name_key;
DROP INDEX courthouse_name_unique_idx;

INSERT INTO darts.courthouse(
cth_id, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name)
select cth_id+1000000, courthouse_code, courthouse_name, created_ts, last_modified_ts, created_by, last_modified_by, display_name from darts.courthouse where cth_id between 1 and 151;

--repoint foreign keys to new record
--"court_case"
update darts.court_case set cth_id = cth_id+1000000 where cth_id between 1 and 151;
--"courthouse_region_ae"
delete from courthouse_region_ae; --repopulating in next script
--"courtroom"
update darts.courtroom set cth_id = cth_id+1000000 where cth_id between 1 and 151;
--"security_group_courthouse_ae"
update darts.security_group_courthouse_ae set cth_id = cth_id+1000000 where cth_id between 1 and 151;
--delete old record
delete from darts.courthouse where cth_id between 1 and 151;

