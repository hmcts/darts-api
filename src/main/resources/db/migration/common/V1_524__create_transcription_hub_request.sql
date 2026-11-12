CREATE TABLE transcription_hub_request (
    tra_id BIGINT PRIMARY KEY,
    CONSTRAINT transcription_hub_request_transcription_fk
        FOREIGN KEY (tra_id) REFERENCES transcription (tra_id)
);
