--v46 Removed workflow related elements from transcription.
--v47 Removed column transcription.current_state
--    added column transcription.trs_id and FK to transcription_status

ALTER TABLE transcription
  DROP COLUMN requested_by;
ALTER TABLE transcription
  DROP COLUMN approved_by;
ALTER TABLE transcription
  DROP COLUMN approved_on_ts;
ALTER TABLE transcription
  DROP COLUMN transcribed_by;

ALTER TABLE transcription
  ADD COLUMN trs_id INTEGER NOT NULL;

COMMENT ON COLUMN transcription.trs_id
  IS 'foreign key to transcription_status';

ALTER TABLE transcription
  ADD CONSTRAINT transcription_transcription_status_fk
    FOREIGN KEY (trs_id) REFERENCES transcription_status (trs_id);
