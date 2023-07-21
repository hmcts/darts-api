ALTER TABLE darts.event_handler DROP COLUMN last_modified_by;
ALTER TABLE darts.event ALTER COLUMN event_id TYPE INTEGER;
ALTER TABLE darts.user_account ALTER COLUMN user_state TYPE INTEGER;
