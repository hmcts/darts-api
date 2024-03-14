INSERT INTO darts.automated_task (aut_id,task_name,task_description,cron_expression,cron_editable)
VALUES (12,'CloseOldCases','Closes cases over 6 years old','0 0 0 L * *',true);

ALTER SEQUENCE darts.aut_seq RESTART WITH 12;
