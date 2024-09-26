CREATE INDEX event_event_ts_idx ON darts."event" (event_ts);
CREATE INDEX media_start_ts_idx ON darts.media (start_ts,end_ts);
CREATE INDEX media_media_file_idx ON darts.media (media_file);


