-- Delete the current rows in the object_hidden_reason table
DELETE FROM darts.object_hidden_reason
       WHERE ohr_reason
                 IN ('PUBLIC_INTEREST_IMMUNITY', 'CLASSIFIED', 'OTHER_DELETE', 'OTHER_HIDE');

-- Insert the new values into the object_hidden_reason table
INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (1, 'PUBLIC_INTEREST_IMMUNITY', 'Public interest immunity', true, 1, true);

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (2, 'CLASSIFIED', 'Classified above official', true, 2, true);

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (3, 'OTHER_DELETE', 'Other reason to delete', true, 3, true);

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (4, 'OTHER_HIDE', 'Other reason to hide only', true, 4, false);
