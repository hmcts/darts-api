-- Table: darts_schema.moj_media

-- DROP TABLE IF EXISTS darts_schema.moj_media;

CREATE TABLE IF NOT EXISTS darts_schema.moj_media
(
    moj_med_id INTEGER NOT NULL,
    moj_crt_id INTEGER,
    r_media_object_id text COLLATE pg_catalog."default",
    c_channel INTEGER,
    c_total_channels INTEGER,
    c_reference_id text COLLATE pg_catalog."default",
    c_start TIMESTAMP without TIME ZONE,
    c_end TIMESTAMP without TIME ZONE,
    c_courtroom text COLLATE pg_catalog."default",
    c_reporting_restrictions INTEGER,
    c_case_id text COLLATE pg_catalog."default",
    r_case_object_id text COLLATE pg_catalog."default",
    r_version_label text COLLATE pg_catalog."default",
    i_superseded boolean,
    i_version_label SMALLINT,
    CONSTRAINT moj_media_pkey PRIMARY KEY (moj_med_id)
)
