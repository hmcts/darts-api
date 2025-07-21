--Add new object record status for deleted objects. Manully set the ors_id to ensure it aligns with the enum value
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (25, 'Deleted from datastore');
--Update the sequence to ensure it continues from the last inserted value
alter sequence ors_seq restart with 26;
