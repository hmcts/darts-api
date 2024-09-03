-- Run every hour at 10 mins past the hour between 8pm and 7am with batch size 5,000
update automated_task set cron_expression = '0 10 20-22,23,0-7 * * *', batch_size = 5000
where task_name = 'CloseOldCases';

-- Run every hour at 30 mins past the hour between 8pm and 7am with batch size 5,000
update automated_task set cron_expression = '0 30 20-22,23,0-7 * * *', batch_size = 5000
where task_name = 'GenerateCaseDocument';

-- Run every hour at 50 mins past the hour between 8pm and 7am with batch size 5,000
update automated_task set cron_expression = '0 50 20-22,23,0-7 * * *', batch_size = 5000
where task_name = 'GenerateCaseDocumentForRetentionDate';

-- Run every hour at 15 mins past the hour
update automated_task set cron_expression = '0 15 * * * *'
where task_name = 'ApplyRetentionCaseAssociatedObjects';

-- Run every hour at 45 mins past the hour
update automated_task set cron_expression = '0 45 * * * *'
where task_name = 'ArmRetentionEventDateCalculator';

-- Run every day at 10pm with batch size 500
update automated_task set cron_expression = '0 0 22 * * *', batch_size = 500
where task_name = 'BatchCleanupArmResponseFiles';

-- Run every day at 11pm
update automated_task set cron_expression = '0 0 23 * * *'
where task_name = 'CleanupArmResponseFiles';

-- Run every 5 mins from 1 minute past with batch size 500
update automated_task set cron_expression = '0 1/5 * * * *', batch_size = 500
where task_name = 'CleanupCurrentEvent';

-- Run every day at 20:15
update automated_task set cron_expression = '0 15 20 * * *'
where task_name = 'CleanupCurrentEvent';

-- Run every day at 21:03
update automated_task set cron_expression = '0 3 21 * * *'
where task_name = 'DailyListHousekeeping';

-- Run every day at 20:30
update automated_task set cron_expression = '0 30 20 * * *'
where task_name = 'ExternalDataStoreDeleter';

-- Run every day at 22:04
update automated_task set cron_expression = '0 4 22 * * *'
where task_name = 'InboundAudioDeleter';

-- Run every day at 22:34
update automated_task set cron_expression = '0 34 22 * * *'
where task_name = 'InboundTranscriptionAnnotationDeleter';

-- Run every day at 23:04
update automated_task set cron_expression = '0 4 23 * * *'
where task_name = 'OutboundAudioDeleter';

-- Run every hour at 7 mins past the hour with batch size 100
update automated_task set cron_expression = '0 7 * * * *', batch_size = 100
where task_name = 'UnstructuredToArmDataStore';

-- Run every hour at 44 mins past the hour with batch size 1000
update automated_task set cron_expression = '0 44 * * * *', batch_size = 1000
where task_name = 'ProcessArmResponseFiles';

-- Run every day at 22:37
update automated_task set cron_expression = '0 37 22 * * *'
where task_name = 'ProcessDailyList';

-- Run every day at 22:27
update automated_task set cron_expression = '0 27 22 * * *'
where task_name = 'UnstructuredAudioDeleter';

-- Run every day at 22:57
update automated_task set cron_expression = '0 57 22 * * *'
where task_name = 'UnstructuredTranscriptionAnnotationDeleter';
