INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable)
VALUES (2, 'CloseOldUnfinishedTranscriptions', 'Close transcriptions that are old and not in a finished state', '0 20 11 * * *', true);

ALTER SEQUENCE aut_seq RESTART WITH 3;
