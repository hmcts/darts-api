-- Run every day at 3am
update automated_task
set cron_expression = '0 3 * * * *'
where task_name = 'CaseExpiryDeletion';

-- Run every day at 4am
update automated_task
set cron_expression = '0 4 * * * *'
where task_name = 'AssociatedObjectDataExpiryDeletion';