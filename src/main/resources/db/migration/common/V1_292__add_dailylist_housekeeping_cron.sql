INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable)
VALUES (13, 'DailyListHousekeeping', 'Deletes daily lists older than 30 days', '0 30 16 * * *', true);

ALTER SEQUENCE darts.aut_seq RESTART WITH 14;