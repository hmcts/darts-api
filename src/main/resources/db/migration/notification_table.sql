-- Table: darts_schema.notification

-- DROP TABLE IF EXISTS darts_schema.notification;

CREATE TABLE IF NOT EXISTS darts_schema.notification
(
    id integer NOT NULL,
    event_id text COLLATE pg_catalog."default" NOT NULL,
    case_id text COLLATE pg_catalog."default" NOT NULL,
    email_address text COLLATE pg_catalog."default" NOT NULL,
    status text COLLATE pg_catalog."default" NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    template_values text COLLATE pg_catalog."default",
    created_date_time timestamp without time zone NOT NULL,
    last_updated_date_time timestamp without time zone NOT NULL,
    CONSTRAINT notification_pkey PRIMARY KEY (id)
)

