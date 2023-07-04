ALTER TABLE notification ALTER created_date_time TYPE TIMESTAMP WITH TIME ZONE;
ALTER TABLE notification ALTER last_updated_date_time TYPE TIMESTAMP WITH TIME ZONE;

CREATE SEQUENCE IF NOT EXISTS notification_seq;
