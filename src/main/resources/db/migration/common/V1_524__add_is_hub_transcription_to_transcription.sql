ALTER TABLE transcription
    ADD COLUMN is_hub_transcription BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE transcription_aud
    ADD COLUMN is_hub_transcription BOOLEAN NOT NULL DEFAULT FALSE;
