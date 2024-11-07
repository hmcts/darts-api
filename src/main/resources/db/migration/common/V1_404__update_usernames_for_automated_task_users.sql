DELETE FROM user_account WHERE usr_id = -6;
DELETE FROM user_account WHERE usr_id = -18;

UPDATE user_account
SET description = 'System user for DailyListHousekeeping job', user_name = 'system_DailyListHousekeeping'
WHERE usr_id = -1;

UPDATE user_account
SET description = 'System user for ProcessDailyList job', user_name = 'system_ProcessDailyList'
WHERE usr_id = -2;

UPDATE user_account
SET description = 'System user for CloseOldUnfinishedTranscriptions job', user_name = 'system_CloseOldUnfinishedTranscriptions'
WHERE usr_id = -7;

UPDATE user_account
SET description = 'System user for OutboundAudioDeleter job', user_name = 'system_OutboundAudioDeleter'
WHERE usr_id = -8;

UPDATE user_account
SET description = 'System user for InboundAudioDeleter job', user_name = 'system_OutboundAudioDeleter'
WHERE usr_id = -9;

UPDATE user_account
SET description = 'System user for ExternalDataStoreDeleter job', user_name = 'system_ExternalDataStoreDeleter'
WHERE usr_id = -10;

UPDATE user_account
SET description = 'System user for InboundToUnstructuredDataStore job', user_name = 'system_InboundToUnstructuredDataStore'
WHERE usr_id = -11;

UPDATE user_account
SET description = 'System user for UnstructuredAudioDeleter job', user_name = 'system_UnstructuredAudioDeleter'
WHERE usr_id = -12;

UPDATE user_account
SET description = 'System user for UnstructuredToArmDataStore job', user_name = 'system_UnstructuredToArmDataStore'
WHERE usr_id = -13;

UPDATE user_account
SET description = 'System user for ProcessArmResponseFiles job', user_name = 'system_ProcessArmResponseFiles'
WHERE usr_id = -14;

UPDATE user_account
SET description = 'System user for CleanupArmResponseFiles job', user_name = 'system_CleanupArmResponseFiles'
WHERE usr_id = -15;

UPDATE user_account
SET description = 'System user for ApplyRetention job', user_name = 'system_ApplyRetention'
WHERE usr_id = -16;

UPDATE user_account
SET description = 'System user for CloseOldCases job', user_name = 'system_CloseOldCases'
WHERE usr_id = -17;

UPDATE user_account
SET description = 'System user for ArmRetentionEventDateCalculator job', user_name = 'system_ArmRetentionEventDateCalculator'
WHERE usr_id = -19;

UPDATE user_account
SET description = 'System user for ApplyRetentionCaseAssociatedObjects job', user_name = 'system_ApplyRetentionCaseAssociatedObjects'
WHERE usr_id = -20;

UPDATE user_account
SET description = 'System user for BatchCleanupArmResponseFiles job', user_name = 'system_BatchCleanupArmResponseFiles'
WHERE usr_id = -21;

UPDATE user_account
SET description = 'System user for GenerateCaseDocument job', user_name = 'system_GenerateCaseDocument'
WHERE usr_id = -22;

UPDATE user_account
SET description = 'System user for RemoveDuplicatedEvents job', user_name = 'system_RemoveDuplicatedEvents'
WHERE usr_id = -23;

UPDATE user_account
SET description = 'System user for InboundTranscriptionAnnotationDeleter job', user_name = 'system_InboundTranscriptionAnnotationDeleter'
WHERE usr_id = -24;

UPDATE user_account
SET description = 'System user for UnstructuredTranscriptionAnnotationDeleter job', user_name = 'system_UnstructuredTranscriptionAnnotationDeleter'
WHERE usr_id = -25;

UPDATE user_account
SET description = 'System user for GenerateCaseDocumentForRetentionDate job', user_name = 'system_GenerateCaseDocumentForRetentionDate'
WHERE usr_id = -26;

UPDATE user_account
SET description = 'System user for CaseExpiryDeletion job', user_name = 'system_CaseExpiryDeletion'
WHERE usr_id = -27;

UPDATE user_account
SET description = 'System user for AssociatedObjectDataExpiryDeletion job', user_name = 'system_AssociatedObjectDataExpiryDeletion'
WHERE usr_id = -28;

UPDATE user_account
SET description = 'System user for ProcessDETSToArmResponse job', user_name = 'system_ProcessDETSToArmResponse'
WHERE usr_id = -29;

UPDATE user_account
SET description = 'System user for ManualDeletion job', user_name = 'system_ManualDeletion'
WHERE usr_id = -30;

UPDATE user_account
SET description = 'System user for DetsToArm job', user_name = 'system_DetsToArm'
WHERE usr_id = -31;

UPDATE user_account
SET description = 'System user for AudioLinking job', user_name = 'system_AudioLinking'
WHERE usr_id = -32;

UPDATE user_account
SET description = 'System user for ProcessE2EArmRpoPending job', user_name = 'system_ProcessE2EArmRpoPending'
WHERE usr_id = -33;

UPDATE user_account
SET description = 'System user for ArmRpoPolling job', user_name = 'system_ArmRpoPolling'
WHERE usr_id = -34;

UPDATE user_account
SET description = 'System user for ArmRpoReplay job', user_name = 'system_ArmRpoReplay'
WHERE usr_id = -35;

UPDATE user_account
SET description = 'System user for ProcessARMRPOPending job', user_name = 'system_ProcessARMRPOPending'
WHERE usr_id = -36;
