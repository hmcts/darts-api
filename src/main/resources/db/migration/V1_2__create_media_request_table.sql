CREATE TABLE IF NOT EXISTS moj_media_request (
  moj_mer_id                INTEGER                     NOT NULL
, moj_hea_id                INTEGER                     NOT NULL
, requestor                 INTEGER                     NOT NULL
, request_status            CHARACTER VARYING           NOT NULL
, request_type              CHARACTER VARYING           NOT NULL
, req_proc_attempts         INTEGER                     NOT NULL DEFAULT 0
, start_ts                  TIMESTAMP WITH TIME ZONE    NOT NULL
, end_ts                    TIMESTAMP WITH TIME ZONE    NOT NULL
, outbound_location         CHARACTER VARYING
, output_format             CHARACTER VARYING
, output_filename           CHARACTER VARYING
, last_accessed_ts          TIMESTAMP WITH TIME ZONE    -- when the output file is accessed (DOWNLOAD, PLAYBACK)
, created_ts                TIMESTAMP WITH TIME ZONE    NOT NULL
, last_updated_ts           TIMESTAMP WITH TIME ZONE    NOT NULL
, CONSTRAINT media_request_pkey PRIMARY KEY (moj_mer_id)
);

CREATE SEQUENCE IF NOT EXISTS moj_mer_seq;
