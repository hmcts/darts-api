package uk.gov.hmcts.darts.task.api;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * The task names map directly to the task names in the table automated_tasks, so there should only be one task per name.
 */
@Getter
public enum AutomatedTaskName {
    PROCESS_DAILY_LIST_TASK_NAME("ProcessDailyList"),
    CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME("CloseOldUnfinishedTranscriptions"),
    OUTBOUND_AUDIO_DELETER_TASK_NAME("OutboundAudioDeleter"),
    INBOUND_TO_UNSTRUCTURED_TASK_NAME("InboundToUnstructuredDataStore"),
    INBOUND_AUDIO_DELETER_TASK_NAME("InboundAudioDeleter"),
    EXTERNAL_DATASTORE_DELETER_TASK_NAME("ExternalDataStoreDeleter"),
    UNSTRUCTURED_AUDIO_DELETER_TASK_NAME("UnstructuredAudioDeleter"),
    UNSTRUCTURED_TO_ARM_TASK_NAME("UnstructuredToArmDataStore"),
    PROCESS_ARM_RESPONSE_FILES_TASK_NAME("ProcessArmResponseFiles"),
    PROCESS_DETS_TO_ARM_RESPONSE("ProcessDETSToArmResponse"),
    APPLY_RETENTION_TASK_NAME("ApplyRetention"),
    APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_TASK_NAME("ApplyRetentionCaseAssociatedObjects"),
    CLEANUP_ARM_RESPONSE_FILES_TASK_NAME("CleanupArmResponseFiles"),
    BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME("BatchCleanupArmResponseFiles"),
    CLOSE_OLD_CASES_TASK_NAME("CloseOldCases"),
    DAILY_LIST_HOUSEKEEPING_TASK_NAME("DailyListHousekeeping"),
    ARM_RETENTION_EVENT_DATE_CALCULATOR_TASK_NAME("ArmRetentionEventDateCalculator"),
    GENERATE_CASE_DOCUMENT_TASK_NAME("GenerateCaseDocument"),
    INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME("InboundTranscriptionAnnotationDeleter"),
    UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_TASK_NAME("UnstructuredTranscriptionAnnotationDeleter"),
    REMOVE_DUPLICATED_EVENTS_TASK_NAME("RemoveDuplicatedEvents"),
    GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_TASK_NAME("GenerateCaseDocumentForRetentionDate"),
    DETS_TO_ARM_TASK_NAME("DetsToArm"),
    CASE_EXPIRY_DELETION_TASK_NAME("CaseExpiryDeletion", "${darts.automated.task.case-expiry-deletion.enabled:false}"),
    ASSOCIATED_OBJECT_DATA_EXPIRY_DELETION_TASK_NAME("AssociatedObjectDataExpiryDeletion"),
    MANUAL_DELETION("ManualDeletion", "${darts.manual-deletion.enabled:false}"),
    PROCESS_ARM_RPO_PENDING("ProcessARMRPOPending", "!${darts.automated.task.process-e2e-arm-rpo-pending.process-e2e-arm-rpo:false}"),
    AUDIO_LINKING_TASK_NAME("AudioLinking"),
    PROCESS_E2E_ARM_PENDING_TASK_NAME("ProcessE2EArmRpoPending"),
    ARM_RPO_POLL_TASK_NAME("ArmRpoPoll"),
    ARM_RPO_REPLAY_TASK_NAME("ArmRpoReplay");
    
    private final String taskName;
    private final String conditionalOnSpEL;

    private static final Map<String, AutomatedTaskName> BY_TASK_NAME = new HashMap<>();

    static {
        for (AutomatedTaskName autTaskName : values()) {
            BY_TASK_NAME.put(autTaskName.taskName, autTaskName);
        }
    }

    AutomatedTaskName(String taskName) {
        this(taskName, null);
    }

    AutomatedTaskName(String taskName, String conditionalOnSpEL) {
        this.taskName = taskName;
        this.conditionalOnSpEL = conditionalOnSpEL;
    }


    public static AutomatedTaskName valueOfTaskName(String taskName) {
        return BY_TASK_NAME.get(taskName);
    }
}