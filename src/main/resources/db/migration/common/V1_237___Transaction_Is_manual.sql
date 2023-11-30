-- swap the values around in transaction for the is_manual column
UPDATE transcription
set is_manual=false
WHERE trt_id = 999;
UPDATE transcription
set is_manual=true
WHERE trt_id <> 999;
-- change the name to is_manual_transcription
ALTER TABLE transcription
RENAME COLUMN is_manual TO is_manual_transcription;
