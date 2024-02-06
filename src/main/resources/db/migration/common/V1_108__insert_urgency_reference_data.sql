INSERT INTO darts.urgency(urg_id, description)
VALUES (1, 'Standard');
INSERT INTO darts.urgency(urg_id, description)
VALUES (2, 'Overnight');
INSERT INTO darts.urgency(urg_id, description)
VALUES (3, 'Other');

ALTER SEQUENCE urg_seq RESTART WITH 4;


