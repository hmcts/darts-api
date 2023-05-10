-- Table: darts_schema.audio_request

-- DROP TABLE IF EXISTS darts_schema.audio_request;

CREATE TABLE IF NOT EXISTS darts_schema.audio_request
(
    request_id integer NOT NULL,
    case_id text COLLATE pg_catalog."default" NOT NULL,
    email_address text COLLATE pg_catalog."default" NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    request_type text COLLATE pg_catalog."default" NOT NULL,
    status text COLLATE pg_catalog."default" NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    created_date_time timestamp without time zone NOT NULL,
    last_updated_date_time timestamp without time zone NOT NULL,
    CONSTRAINT audio_request_pkey PRIMARY KEY (request_id)
)
