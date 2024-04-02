ALTER TABLE case_transcription_ae ADD CONSTRAINT case_transcription_ae_pk PRIMARY KEY (cas_id,tra_id);
ALTER TABLE hearing_transcription_ae ADD CONSTRAINT hearing_transcription_ae_pk PRIMARY KEY (hea_id,tra_id);
ALTER TABLE object_hidden_reason ADD CONSTRAINT object_hidden_reason_pk PRIMARY KEY (ohr_id);

