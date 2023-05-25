CREATE TABLE IF NOT EXISTS audio_request (
  request_id             integer                     NOT NULL
, case_id                text                        NOT NULL
, requester              text                        NOT NULL
, start_time             timestamp without time zone NOT NULL
, end_time               timestamp without time zone NOT NULL
, request_type           text                        NOT NULL
, status                 text                        NOT NULL
, attempts               integer                     NOT NULL DEFAULT 0
, outbound_location      text                        NULL
, created_date_time      timestamp without time zone NOT NULL
, last_updated_date_time timestamp without time zone NOT NULL
, CONSTRAINT audio_request_pkey PRIMARY KEY (request_id)
)
