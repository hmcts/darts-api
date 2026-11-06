CREATE TABLE job_last_successful_run_time
(
    job_name                          CHARACTER VARYING(255)   NOT NULL,
    last_successful_run_timestamp_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT job_last_successful_run_time_pk PRIMARY KEY (job_name)
);
