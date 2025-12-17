-- Run every hour at 15 mins past the hour
update automated_task set cron_expression = '0 15 * * * *'
where task_name = 'DetsToArm';

-- Run every hour at 11 mins and 44 mins past the hour
update automated_task set cron_expression = '0 11/30 * * * *'
where task_name = 'ProcessDETSToArmResponse';
