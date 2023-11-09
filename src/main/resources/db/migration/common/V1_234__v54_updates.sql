ALTER TABLE annotation_document ADD COLUMN checksum     CHARACTER VARYING;
UPDATE annotation_document
set checksum='a-checksum';
ALTER TABLE annotation_document ALTER COLUMN checksum SET NOT NULL;

ALTER TABLE transcription_document ADD COLUMN checksum     CHARACTER VARYING;
UPDATE transcription_document
set checksum='a-checksum';
ALTER TABLE transcription_document ALTER COLUMN checksum SET NOT NULL;

UPDATE security_role
set display_name='Approver', display_state=true
WHERE rol_id = 1;

UPDATE security_role
set display_name='Requestor', display_state=true
WHERE rol_id = 2;

UPDATE security_role
set display_name='Judge', display_state=true
WHERE rol_id = 3;

UPDATE security_role
set display_name='Transcriber', display_state=true
WHERE rol_id = 4;

UPDATE security_role
set display_name='Language Shop', display_state=true
WHERE rol_id = 5;

UPDATE security_role
set display_name='RCJ Appeals', display_state=true
WHERE rol_id = 6;

UPDATE security_role
set display_name='XHIBIT', display_state=true
WHERE rol_id = 7;

UPDATE security_role
set display_name='CPP', display_state=true
WHERE rol_id = 8;

UPDATE security_role
set display_name='DAR PC', display_state=true
WHERE rol_id = 9;

UPDATE security_role
set display_name='Mid Tier', display_state=true
WHERE rol_id = 10;

ALTER TABLE transcription_status ADD COLUMN display_name     CHARACTER VARYING;
UPDATE transcription_status
SET display_name='Requested'
WHERE trs_id=1;
UPDATE transcription_status
SET display_name='Awaiting Authorisation'
WHERE trs_id=2;
UPDATE transcription_status
SET display_name='Approved'
WHERE trs_id=3;
UPDATE transcription_status
SET display_name='Rejected'
WHERE trs_id=4;
UPDATE transcription_status
SET display_name='With Transcriber'
WHERE trs_id=5;
UPDATE transcription_status
SET display_name='Complete'
WHERE trs_id=6;
UPDATE transcription_status
SET display_name='Closed'
WHERE trs_id=7;
ALTER TABLE transcription_status ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE transcription_type ADD COLUMN display_state     BOOLEAN;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=1;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=2;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=3;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=4;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=5;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=6;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=7;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=8;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=9;
UPDATE transcription_type
SET display_state=true
WHERE trt_id=999;
ALTER TABLE transcription_type ALTER COLUMN display_state SET NOT NULL;

ALTER TABLE transcription_urgency ADD COLUMN display_state     BOOLEAN DEFAULT TRUE ;
UPDATE transcription_urgency
SET display_state=false
WHERE tru_id=1;
UPDATE transcription_urgency
SET display_state=false
WHERE tru_id=3;

INSERT INTO transcription_urgency (tru_id, description, display_state) VALUES (4, '3 working days', true);
INSERT INTO transcription_urgency (tru_id, description, display_state) VALUES (5, '7 working days', true);
INSERT INTO transcription_urgency (tru_id, description, display_state) VALUES (6, '12 working days', true);
ALTER TABLE transcription_urgency ALTER COLUMN display_state SET NOT NULL;
