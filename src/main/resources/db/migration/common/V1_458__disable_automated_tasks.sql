UPDATE automated_task
SET task_enabled = false
WHERE task_name in ('ApplyRetention', 'GenerateCaseDocument', 'GenerateCaseDocumentForRetentionDate','ApplyRetentionCaseAssociatedObjects');
