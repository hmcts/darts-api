update darts.automated_task set cron_expression = '0 10 0-8,20-23 ? * *' where task_name = 'CloseOldCases';
update darts.automated_task set cron_expression = '0 0,18 0-8,20-23 ? * *' where task_name = 'BatchCleanupArmResponseFiles';
update darts.automated_task set cron_expression = '0 0,30 0-8,20-23 ? * *' where task_name = 'GenerateCaseDocument';
update darts.automated_task set cron_expression = '0 50 0-8,20-23 ? * *' where task_name = 'GenerateCaseDocumentForRetentionDate';
update darts.automated_task set cron_expression = '0 9 0-6,21-23 ? * *' where task_name = 'ProcessDETSToArmResponse';
update darts.automated_task set cron_expression = '0 9 0-6,21-23 ? * *' where task_name = 'DetsToArm';
update darts.automated_task set cron_expression = '0 0 0-8,20-23 ? * *' where task_name = 'AudioLinking';
