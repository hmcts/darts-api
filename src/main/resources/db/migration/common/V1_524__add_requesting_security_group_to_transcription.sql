ALTER TABLE transcription
    ADD COLUMN requesting_group_id INTEGER;

ALTER TABLE transcription
    ADD CONSTRAINT transcription_requesting_group_fk
        FOREIGN KEY (requesting_group_id) REFERENCES security_group(grp_id);

ALTER TABLE transcription_aud
    ADD COLUMN requesting_group_id INTEGER;

CREATE INDEX transcription_requesting_group_idx
    ON transcription (requesting_group_id);
