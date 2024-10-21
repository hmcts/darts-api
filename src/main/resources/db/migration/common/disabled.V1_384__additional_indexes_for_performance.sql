CREATE INDEX IF NOT EXISTS event_event_ts_idx ON darts."event" (event_ts);
CREATE INDEX IF NOT EXISTS media_start_ts_idx ON darts.media (start_ts, end_ts);
CREATE INDEX IF NOT EXISTS media_media_file_idx ON darts.media (media_file);
CREATE INDEX IF NOT EXISTS event_event_id_idx ON darts."event" (event_id, is_current);
