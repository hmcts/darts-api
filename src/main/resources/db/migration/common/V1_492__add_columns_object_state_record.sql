ALTER TABLE IF EXISTS object_state_record ADD COLUMN flag_file_stored_in_arm BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS object_state_record ADD COLUMN date_file_stored_in_arm TIMESTAMP WITH TIME ZONE;
