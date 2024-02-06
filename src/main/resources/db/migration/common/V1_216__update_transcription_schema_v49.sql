ALTER TABLE transcription
  ALTER COLUMN trs_id DROP NOT NULL;
ALTER TABLE transcription
  DROP COLUMN company;
ALTER TABLE transcription
  DROP COLUMN current_state;
ALTER TABLE transcription
  DROP COLUMN current_state_ts;


ALTER TABLE transcription_status
  ADD COLUMN created_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE transcription_status
  ADD COLUMN created_by INTEGER;
ALTER TABLE transcription_status
  ADD COLUMN last_modified_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE transcription_status
  ADD COLUMN last_modified_by INTEGER;

UPDATE transcription_status
SET status_type='Requested',
    created_by=0,
    last_modified_by=0,
    created_ts=now(),
    last_modified_ts=now()
WHERE trs_id = 1;
UPDATE transcription_status
SET status_type='Awaiting Authorisation',
    created_by=0,
    last_modified_by=0,
    created_ts=now(),
    last_modified_ts=now()
WHERE trs_id = 2;
UPDATE transcription_status
SET status_type='Approved',
    created_by=0,
    last_modified_by=0,
    created_ts=now(),
    last_modified_ts=now()
WHERE trs_id = 3;
UPDATE transcription_status
SET status_type='Rejected',
    created_by=0,
    last_modified_by=0,
    created_ts=now(),
    last_modified_ts=now()
WHERE trs_id = 4;
UPDATE transcription_status
SET status_type='With Transcriber',
    created_by=0,
    last_modified_by=0,
    created_ts=now(),
    last_modified_ts=now()
WHERE trs_id = 5;
UPDATE transcription_status
SET status_type='Complete',
    created_by=0,
    last_modified_by=0,
    created_ts=now(),
    last_modified_ts=now()
WHERE trs_id = 6;
INSERT INTO transcription_status (trs_id, status_type, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (7, 'Closed', now(), 0, now(), 0);

ALTER TABLE transcription_status
  ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE transcription_status
  ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE transcription_status
  ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE transcription_status
  ALTER COLUMN last_modified_by SET NOT NULL;


ALTER TABLE transcription_workflow
  ADD COLUMN trs_id INTEGER;
ALTER TABLE transcription_workflow
  ADD COLUMN workflow_actor INTEGER;
ALTER TABLE transcription_workflow
  ADD COLUMN workflow_ts TIMESTAMP WITH TIME ZONE;

UPDATE transcription_workflow
SET trs_id=1,
    workflow_actor=0,
    workflow_ts=now()
WHERE workflow_stage = 'REQUESTED';

ALTER TABLE transcription_workflow
  DROP COLUMN workflow_stage;
ALTER TABLE transcription_workflow
  ALTER COLUMN trs_id SET NOT NULL;
ALTER TABLE transcription_workflow
  ALTER COLUMN workflow_actor SET NOT NULL;
ALTER TABLE transcription_workflow
  ALTER COLUMN workflow_ts SET NOT NULL;


DROP SEQUENCE trs_seq;
DROP SEQUENCE trt_seq;
DROP SEQUENCE tru_seq;
