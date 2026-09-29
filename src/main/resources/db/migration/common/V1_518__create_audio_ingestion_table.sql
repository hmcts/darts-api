CREATE TABLE audio_ingestion (
    id BIGSERIAL PRIMARY KEY,
    source_full_filename VARCHAR(1000) NOT NULL,
    source_last_modified_ts TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    claim_owner VARCHAR(255),
    claimed_at TIMESTAMP WITH TIME ZONE,
    error TEXT,
    created_ts TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT audio_ingestion_status_chk
        CHECK (status IN ('NEW', 'PROCESSING', 'FAILED')),

    CONSTRAINT audio_ingestion_source_filename_uk
        UNIQUE (source_full_filename)
);