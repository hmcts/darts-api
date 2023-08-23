INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (1,'ProcessDailyList','Process the latest daily list for each courthouse','0 5 2 * * *',true);

ALTER SEQUENCE aut_seq RESTART WITH 2;
