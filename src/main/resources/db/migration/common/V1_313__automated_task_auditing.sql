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