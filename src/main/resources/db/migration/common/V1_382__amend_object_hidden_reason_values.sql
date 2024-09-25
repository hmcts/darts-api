-- Update existing rows in the object_hidden_reason table

UPDATE darts.object_hidden_reason
SET display_state = true
WHERE ohr_id IN (1, 2, 3);
