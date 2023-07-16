CREATE TABLE IF NOT EXISTS automated_task
(aut_id                     INTEGER                    NOT NULL
,task_name                  CHARACTER VARYING          NOT NULL
,task_description                CHARACTER VARYING          NOT NULL
,cron_expression            CHARACTER VARYING          NOT NULL
,cron_editable              CHARACTER VARYING          NOT NULL
, CONSTRAINT automated_task_pkey PRIMARY KEY (aut_id)
);

COMMENT ON COLUMN darts.automated_task.aut_id
IS 'primary key of automated_tasks';

CREATE SEQUENCE IF NOT EXISTS automated_task_seq;



