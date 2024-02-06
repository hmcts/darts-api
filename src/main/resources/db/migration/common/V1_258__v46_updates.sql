ALTER TABLE event
  ADD COLUMN is_log_entry BOOLEAN NOT NULL default false;

ALTER TABLE external_object_directory
  ADD COLUMN external_file_id CHARACTER VARYING;
ALTER TABLE external_object_directory
  ADD COLUMN external_record_id CHARACTER VARYING;

ALTER TABLE automated_task
  ADD COLUMN task_enabled BOOLEAN NOT NULL default true;

ALTER TABLE report
  DROP COLUMN superseded;
ALTER TABLE report
  DROP COLUMN version;
