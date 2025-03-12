ALTER TABLE object_state_record ALTER COLUMN eod_id TYPE INTEGER USING (eod_id::integer);
ALTER TABLE object_state_record ALTER COLUMN arm_eod_id TYPE INTEGER USING (arm_eod_id::integer);
