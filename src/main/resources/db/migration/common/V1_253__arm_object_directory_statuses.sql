
INSERT INTO object_record_status (ors_id,ors_description) VALUES (13,'Arm Drop Zone');
INSERT INTO object_record_status (ors_id,ors_description) VALUES (14,'Arm Raw Data Failed');
INSERT INTO object_record_status (ors_id,ors_description) VALUES (15,'Arm Manifest Failed');
ALTER SEQUENCE ors_seq RESTART WITH 16;

-- Fix statuses that have now changed
UPDATE darts.external_object_directory
SET ors_id = 15
WHERE elt_id = 3
AND ors_id in (10,12);

UPDATE darts.external_object_directory
SET ors_id = 14
WHERE elt_id = 3
AND ors_id = 8;
