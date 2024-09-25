-- Update existing rows in the object_hidden_reason table

UPDATE darts.object_hidden_reason
SET display_state = true
WHERE ohr_id IN (1, 2, 3);

-- Optionally insert new values if they don't already exist
INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
SELECT 1, 'PUBLIC_INTEREST_IMMUNITY', 'Public interest immunity', true, 1, true
WHERE NOT EXISTS (SELECT 1 FROM darts.object_hidden_reason WHERE ohr_reason = 'PUBLIC_INTEREST_IMMUNITY');

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
SELECT 2, 'CLASSIFIED', 'Classified above official', true, 2, true
WHERE NOT EXISTS (SELECT 1 FROM darts.object_hidden_reason WHERE ohr_reason = 'CLASSIFIED');

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
SELECT 3, 'OTHER_DELETE', 'Other reason to delete', true, 3, true
WHERE NOT EXISTS (SELECT 1 FROM darts.object_hidden_reason WHERE ohr_reason = 'OTHER_DELETE');

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
SELECT 4, 'OTHER_HIDE', 'Other reason to hide only', true, 4, false
WHERE NOT EXISTS (SELECT 1 FROM darts.object_hidden_reason WHERE ohr_reason = 'OTHER_HIDE');
