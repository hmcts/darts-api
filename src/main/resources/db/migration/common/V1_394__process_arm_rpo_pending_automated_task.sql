delete from darts.arm_automated_task where aat_id in (1, 2);

update darts.automated_task a set aut_id = a.aut_id + 1000; --Required to avoid conflict with existing automated task ids
update darts.automated_task set aut_id = 1 where task_name = 'ProcessDailyList';
update darts.automated_task set aut_id = 2 where task_name = 'CloseOldUnfinishedTranscriptions';
update darts.automated_task set aut_id = 3 where task_name = 'OutboundAudioDeleter';
update darts.automated_task set aut_id = 4 where task_name = 'InboundAudioDeleter';
update darts.automated_task set aut_id = 5 where task_name = 'ExternalDataStoreDeleter';
update darts.automated_task set aut_id = 6 where task_name = 'InboundToUnstructuredDataStore';
update darts.automated_task set aut_id = 7 where task_name = 'UnstructuredAudioDeleter';
update darts.automated_task set aut_id = 8 where task_name = 'UnstructuredToArmDataStore';
update darts.automated_task set aut_id = 9 where task_name = 'ProcessArmResponseFiles';
update darts.automated_task set aut_id = 10 where task_name = 'CleanupArmResponseFiles';
update darts.automated_task set aut_id = 11 where task_name = 'ApplyRetention';
update darts.automated_task set aut_id = 12 where task_name = 'CloseOldCases';
update darts.automated_task set aut_id = 13 where task_name = 'DailyListHousekeeping';
update darts.automated_task set aut_id = 14 where task_name = 'ArmRetentionEventDateCalculator';
update darts.automated_task set aut_id = 15 where task_name = 'ApplyRetentionCaseAssociatedObjects';
update darts.automated_task set aut_id = 16 where task_name = 'BatchCleanupArmResponseFiles';
update darts.automated_task set aut_id = 17 where task_name = 'GenerateCaseDocument';
update darts.automated_task set aut_id = 18 where task_name = 'RemoveDuplicatedEvents';
update darts.automated_task set aut_id = 19 where task_name = 'InboundTranscriptionAnnotationDeleter';
update darts.automated_task set aut_id = 20 where task_name = 'UnstructuredTranscriptionAnnotationDeleter';
update darts.automated_task set aut_id = 21 where task_name = 'GenerateCaseDocumentForRetentionDate';
update darts.automated_task set aut_id = 22 where task_name = 'CaseExpiryDeletion';
update darts.automated_task set aut_id = 23 where task_name = 'AssociatedObjectDataExpiryDeletion';
update darts.automated_task set aut_id = 24 where task_name = 'ProcessDETSToArmResponse';
update darts.automated_task set aut_id = 25 where task_name = 'ManualDeletion';
update darts.automated_task set aut_id = 26 where task_name = 'DetsToArm';
update darts.automated_task set aut_id = 27 where task_name = 'AudioLinking';


update darts.automated_task set aut_id = 28 where task_name = 'ProcessE2EArmRpoPending';
update darts.automated_task set aut_id = 29 where task_name = 'ArmRpoPolling';
update darts.automated_task set aut_id = 30 where task_name = 'ArmRpoReplay';

INSERT INTO arm_automated_task (aat_id, aut_id, rpo_csv_start_hour, rpo_csv_end_hour, arm_replay_start_ts, arm_replay_end_ts, arm_attribute_type)
VALUES (1, 28, 25, 49, null, null, 'RPO'),
       (2, 30, null, null, null, null, 'REPLAY');


INSERT INTO darts.automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                                  created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (31, 'ProcessARMRPOPending', 'Update the record to STORED which has a status of ARM_RPO_PENDING and data_ingestion_ts is more than 24 hours',
        '0 16 23 * * *', --(16 mins past 11pm every day)
        false, 100000, current_timestamp, 0, current_timestamp, 0);
ALTER SEQUENCE aut_seq RESTART WITH 100;