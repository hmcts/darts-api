UPDATE automated_task
SET task_name = 'ProcessE2EArmRpoPending'
WHERE task_name = 'ProcessE2EArmRpoPendingAutomatedTasks';

UPDATE automated_task
SET task_name = 'ArmRpoPolling'
WHERE task_name = 'ArmRpoPollAutomatedTask';

UPDATE automated_task
SET task_name = 'ArmRpoReplay'
WHERE task_name = 'ArmRpoReplayAutomatedTask';
