CREATE TABLE IF NOT EXISTS notification
(
  id                     integer                     NOT NULL,
  event_id               text                        NOT NULL,
  case_id                text                        NOT NULL,
  email_address          text                        NOT NULL,
  status                 text                        NOT NULL,
  attempts               integer                     NOT NULL DEFAULT 0,
  template_values        text,
  created_date_time      timestamp without time zone NOT NULL,
  last_updated_date_time timestamp without time zone NOT NULL,
  CONSTRAINT notification_pkey PRIMARY KEY (id)
)
