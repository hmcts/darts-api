INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (1, 'PUBLIC_INTEREST_IMMUNITY', 'Public interest immunity', false, 1, true);

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (2, 'CLASSIFIED', 'Classified above official', false, 2, true);

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (3, 'OTHER_DELETE', 'Other reason to delete', false, 3, true);

INSERT INTO darts.object_hidden_reason (ohr_id, ohr_reason, display_name, display_state, display_order, marked_for_deletion)
VALUES (4, 'OTHER_HIDE', 'Other reason to hide only', true, 4, false);
