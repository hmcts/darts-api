create table automated_task_aud
(
    aut_id                     INTEGER                    NOT NULL
    ,task_name                  CHARACTER VARYING          NOT NULL
    ,task_description           CHARACTER VARYING          NOT NULL
    ,cron_expression            CHARACTER VARYING          NOT NULL
    ,cron_editable              BOOLEAN                    NOT NULL
    ,batch_size                 INTEGER
    ,task_enabled  BOOLEAN      NOT NULL                   default true
    ,created_ts                  TIMESTAMP WITH TIME ZONE
    ,created_by                  INTEGER
    ,last_modified_ts            TIMESTAMP WITH TIME ZONE
    ,last_modified_by            INTEGER
    ,rev     integer not null
    ,revtype smallint
    ,primary key (rev, aut_id)
);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Run Job Manually', 'Run Job Manually', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aua_seq'), 'Enable/Disable Job', 'Enable/Disable Job', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);