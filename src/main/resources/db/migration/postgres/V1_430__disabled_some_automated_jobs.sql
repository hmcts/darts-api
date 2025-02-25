update automated_task
set task_enabled = false
where automated_task.task_name in ('ArmRpoPolling', 'ProcessE2EArmRpoPending', 'ArmRpoReplay', 'CaseExpiryDeletion', 'ProcessARMRPOPending');
