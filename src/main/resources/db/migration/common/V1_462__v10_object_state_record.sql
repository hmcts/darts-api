ALTER TABLE object_state_record ALTER COLUMN eod_id TYPE INTEGER USING (eod_id::INTEGER);
ALTER TABLE object_state_record ALTER COLUMN arm_eod_id TYPE INTEGER USING (arm_eod_id::INTEGER);
