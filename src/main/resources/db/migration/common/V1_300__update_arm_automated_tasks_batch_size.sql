UPDATE darts.automated_task set batch_size = 10
WHERE task_name in ('UnstructuredToArmDataStore', 'ProcessArmResponseFiles');
