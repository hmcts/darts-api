ALTER TABLE transcription_workflow
  ADD CONSTRAINT transcription_workflow_pk PRIMARY KEY (trw_id);
ALTER TABLE transcription_status
  ADD CONSTRAINT transcription_status_pk PRIMARY KEY (trs_id);
