CREATE TABLE audio_ingestion_spike_config (
                                              id INTEGER PRIMARY KEY,
                                              temporary_failure_count INTEGER NOT NULL DEFAULT 0
);

INSERT INTO audio_ingestion_spike_config (
    id,
    temporary_failure_count
)
VALUES (1, 0);

-- 0 = normal processing
-- 2 = temporarily fail 2 claimed items
-- 5 = temporarily fail all 5