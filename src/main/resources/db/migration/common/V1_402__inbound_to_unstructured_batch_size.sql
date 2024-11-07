update darts.automated_task
set batch_size = 1000
where task_name = 'InboundToUnstructuredDataStore';
