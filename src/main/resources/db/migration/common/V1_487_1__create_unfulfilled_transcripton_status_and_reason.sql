CREATE TABLE IF NOT EXISTS unfulfilled_transcription_reason
(
    utr_id                      INTEGER                       NOT NULL,
    utr_display_name            CHARACTER VARYING             NOT NULL,
    is_active                   BOOLEAN                       NOT NULL DEFAULT FALSE
);

ALTER TABLE transcription ADD COLUMN utr_id INTEGER;

INSERT INTO transcription_status (trs_id, status_type, display_name) VALUES (8, 'Unfulfilled', 'Unfulfilled');
