-- Run every hour at 9 mins past the hour
update automated_task set cron_expression = '0 9 * * * *'
where task_name = 'DetsToArm';

-- Run every hour at 4 mins and 34 mins past the hour
update automated_task set cron_expression = '0 4/30 * * * *'
where task_name = 'ProcessDETSToArmResponse';
