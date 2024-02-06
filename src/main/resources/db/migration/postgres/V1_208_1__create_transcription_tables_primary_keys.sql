CREATE UNIQUE INDEX transcription_workflow_pk ON transcription_workflow (trw_id);
ALTER TABLE transcription_workflow
  ADD PRIMARY KEY USING INDEX transcription_workflow_pk;

CREATE UNIQUE INDEX transcription_status_pk ON transcription_status (trs_id);
ALTER TABLE transcription_status
  ADD PRIMARY KEY USING INDEX transcription_status_pk;
