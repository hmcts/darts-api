ALTER TABLE transcription ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE transcription ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE transcription ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE transcription ALTER COLUMN last_modified_by SET NOT NULL;


ALTER TABLE transcription_comment ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE transcription_comment ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE transcription_comment ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE transcription_comment ALTER COLUMN last_modified_by SET NOT NULL;


UPDATE transcription_type
SET description='Sentencing remarks', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=1;
UPDATE transcription_type
SET description='Summing up (including verdict)', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=2;
UPDATE transcription_type
SET description='Antecedents', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=3;
UPDATE transcription_type
SET description='Argument and submission of ruling', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=4;
UPDATE transcription_type
SET description='Court Log', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=5;
UPDATE transcription_type
SET description='Mitigation', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=6;
UPDATE transcription_type
SET description='Proceedings after verdict', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=7;
UPDATE transcription_type
SET description='Prosecution opening of facts', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=8;
UPDATE transcription_type
SET description='Specified Times', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=9;
UPDATE transcription_type
SET description='Other', created_by=0, last_modified_by=0, created_ts=now(), last_modified_ts=now()
WHERE trt_id=999;

ALTER TABLE transcription_type ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE transcription_type ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE transcription_type ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE transcription_type ALTER COLUMN last_modified_by SET NOT NULL;


ALTER TABLE transcription_urgency ALTER COLUMN last_modified_by SET NOT NULL;


ALTER TABLE transcription
ADD CONSTRAINT transcription_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_comment
ADD CONSTRAINT transcription_comment_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_type
ADD CONSTRAINT transcription_type_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_type
ADD CONSTRAINT transcription_type_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_urgency
ADD CONSTRAINT transcription_urgency_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_urgency
ADD CONSTRAINT transcription_urgency_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_transcription_status_fk
FOREIGN KEY (trs_id) REFERENCES transcription_status(trs_id);

ALTER TABLE transcription_workflow
ADD CONSTRAINT transcription_workflow_workflow_actor_fk
FOREIGN KEY (workflow_actor) REFERENCES user_account(usr_id);
