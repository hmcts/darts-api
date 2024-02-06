ALTER TABLE transcription
  ADD CONSTRAINT unique_transcription UNIQUE (hea_id, cas_id, trt_id, start_ts, end_ts);
