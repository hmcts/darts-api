-- Run every hour at 9 mins past the hour between 9pm and 12pm the next day
update automated_task set cron_expression = '0 9 0-12,21-23 * * *'
where task_name = 'DetsToArm';

-- Run every hour at 4 mins past the hour between 8pm and 12pm the next day
update automated_task set cron_expression = '0 4 0-12,21-23 * * *'
where task_name = 'ProcessDETSToArmResponse';
