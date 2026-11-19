ALTER TABLE darts.external_object_directory SET (
    autovacuum_vacuum_threshold = 200000,
    autovacuum_vacuum_scale_factor = 0,
    autovacuum_analyze_threshold = 200000,
    autovacuum_analyze_scale_factor = 0
    );

ALTER TABLE darts.object_state_record SET (
    autovacuum_vacuum_threshold = 200000,
    autovacuum_vacuum_scale_factor = 0,
    autovacuum_analyze_threshold = 200000,
    autovacuum_analyze_scale_factor = 0
    );