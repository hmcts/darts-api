CREATE TABLE IF NOT EXISTS moj_media_request (
  moj_mer_id                integer                     NOT NULL
, moj_hea_id                integer                     NOT NULL
, requestor                 integer                     NOT NULL
, start_ts                  timestamp with time zone    NOT NULL
, end_ts                    timestamp with time zone    NOT NULL
, request_type              text                        NOT NULL
, request_status            text                        NOT NULL
, req_proc_attempts         integer                     NOT NULL DEFAULT 0
, outbound_location         text                        NULL
, output_format             text                        NULL
, output_filename           text                        NULL
, last_accessd_ts           timestamp with time zone    NOT NULL
, created_ts                timestamp with time zone    NOT NULL
, last_updated_ts           timestamp with time zone    NOT NULL
, CONSTRAINT media_request_pkey PRIMARY KEY (moj_mer_id)
)
