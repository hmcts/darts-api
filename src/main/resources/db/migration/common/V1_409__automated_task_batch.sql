update darts.automated_task set batch_size = null, cron_expression = '0 5 2 * * ?' where task_name = 'ProcessDailyList';
update darts.automated_task set batch_size = 5000, cron_expression = '0 15 20 * * ?' where task_name = 'CloseOldUnfinishedTranscriptions';
update darts.automated_task set batch_size = 1000, cron_expression = '0 4 23 * * ?' where task_name = 'OutboundAudioDeleter';
update darts.automated_task set batch_size = 10000, cron_expression = '0 4 22 * * ?' where task_name = 'InboundAudioDeleter';
update darts.automated_task set batch_size = 35000, cron_expression = '0 30 20 * * ?' where task_name = 'ExternalDataStoreDeleter';
update darts.automated_task set batch_size = 1000, cron_expression = '* 0/5 * ? * *' where task_name = 'InboundToUnstructuredDataStore';
update darts.automated_task set batch_size = 20000, cron_expression = '0 27 22 * * ?' where task_name = 'UnstructuredAudioDeleter';
update darts.automated_task set batch_size = 2000, cron_expression = '0 7 0/1 ? * *' where task_name = 'UnstructuredToArmDataStore';
update darts.automated_task set batch_size = 200, cron_expression = '* 44 0/1 ? * *' where task_name = 'ProcessArmResponseFiles';
update darts.automated_task set batch_size = null, cron_expression = '0 0 23 * * ?' where task_name = 'CleanupArmResponseFiles';
update darts.automated_task set batch_size = 10000, cron_expression = '* 0 0/1 ? * *' where task_name = 'ApplyRetention';
update darts.automated_task set batch_size = 5000, cron_expression = '0 10 0,1,2,3,4,5,6,7,8,20,21,22,23 ? * *' where task_name = 'CloseOldCases';
update darts.automated_task set batch_size = 1000, cron_expression = '0 3 21 * * ?' where task_name = 'DailyListHousekeeping';
update darts.automated_task set batch_size = 50000, cron_expression = '* 45 0/1 ? * *' where task_name = 'ArmRetentionEventDateCalculator';
update darts.automated_task set batch_size = 10000, cron_expression = '* 15 0/1 ? * *' where task_name = 'ApplyRetentionCaseAssociatedObjects';
update darts.automated_task set batch_size = 500, cron_expression = '* 20 0/1 ? * *' where task_name = 'BatchCleanupArmResponseFiles';
update darts.automated_task set batch_size = 5000, cron_expression = '0 0,30 0,1,2,3,4,5,6,7,8,20,21,22,23 ? * *' where task_name = 'GenerateCaseDocument';
update darts.automated_task set batch_size = 10000, cron_expression = '0 37 22 * * ?' where task_name = 'RemoveDuplicatedEvents';
update darts.automated_task set batch_size = 1000, cron_expression = '0 34 22 * * ?' where task_name = 'InboundTranscriptionAnnotationDeleter';
update darts.automated_task set batch_size = 1000, cron_expression = '0 57 22 * * ?' where task_name = 'UnstructuredTranscriptionAnnotationDeleter';
update darts.automated_task set batch_size = 5000, cron_expression = '0 50 0,1,2,3,4,5,6,7,8,20,21,22,23 ? * *' where task_name = 'GenerateCaseDocumentForRetentionDate';
update darts.automated_task set batch_size = 1000, cron_expression = '0 0 0 31 2 *' where task_name = 'CaseExpiryDeletion';
update darts.automated_task set batch_size = 5000, cron_expression = '0 0 0/1 ? * *' where task_name = 'AssociatedObjectDataExpiryDeletion';
update darts.automated_task set batch_size = 200, cron_expression = '0 0 0/1 ? * *' where task_name = 'ProcessDETSToArmResponse';
update darts.automated_task set batch_size = 5000, cron_expression = '0 0 21 * * ?' where task_name = 'ManualDeletion';
update darts.automated_task set batch_size = 2000, cron_expression = '0 0 0 31 2 *' where task_name = 'DetsToArm';
update darts.automated_task set batch_size = 10000, cron_expression = '0 0 0,1,2,3,4,5,6,7,8,20,21,22,23 ? * *' where task_name = 'AudioLinking';
update darts.automated_task set batch_size = null, cron_expression = '0 0 0 31 2 *' where task_name = 'ProcessE2EArmRpoPending';
update darts.automated_task set batch_size = 100000, cron_expression = '0 0 0 31 2 *' where task_name = 'ArmRpoReplay';