update darts.automated_task
set cron_expression = '0 0 1 * * *';

update darts.automated_task
set cron_expression = '0 */5 * * * *'
where task_name in ('UnstructuredToArmDataStore','InboundToUnstructuredDataStore');