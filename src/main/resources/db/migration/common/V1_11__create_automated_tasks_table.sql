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

INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (1,'AutomatedTaskOne','Simple test of automated task 1','*/10 * * * * *',true);

INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (2,'AutomatedTaskTwo','Simple test of automated task 2','*/5 * * * * *',true);

