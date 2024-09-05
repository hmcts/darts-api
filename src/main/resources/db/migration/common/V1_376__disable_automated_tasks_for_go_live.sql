update automated_task set task_enabled = false
where task_name in (
'CloseOldCases',
'ArmRetentionEventDateCalculator',
'BatchCleanupArmResponseFiles',
'CleanupArmResponseFiles',
'UnstructuredToArmDataStore',
'ProcessArmResponseFiles');

