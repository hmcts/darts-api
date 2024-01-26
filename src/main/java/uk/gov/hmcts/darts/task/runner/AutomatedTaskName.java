package uk.gov.hmcts.darts.task.runner;

import java.util.HashMap;
import java.util.Map;

/**
 * The task names map directly to the task names in the table automated_tasks, so there should only be one task per name.
 */
public enum AutomatedTaskName {
    PROCESS_DAILY_LIST_TASK_NAME("ProcessDailyList"),
    CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS_TASK_NAME("CloseOldUnfinishedTranscriptions"),
    OUTBOUND_AUDIO_DELETER_TASK_NAME("OutboundAudioDeleter"),
    INBOUND_TO_UNSTRUCTURED_TASK_NAME("InboundToUnstructuredDataStore"),
    INBOUND_AUDIO_DELETER_TASK_NAME("InboundAudioDeleter"),
    EXTERNAL_DATASTORE_DELETER("ExternalDataStoreDeleter"),
    UNSTRUCTURED_AUDIO_DELETER_TASK_NAME("UnstructuredAudioDeleter"),
    UNSTRUCTURED_TO_ARM_TASK_NAME("UnstructuredToArmDataStore"),
    PROCESS_ARM_RESPONSE_FILES_TASK_NAME("ProcessArmResponseFiles");

    private final String taskName;

    private static final Map<String, AutomatedTaskName> BY_TASK_NAME = new HashMap<>();

    static {
        for (AutomatedTaskName autTaskName : values()) {
            BY_TASK_NAME.put(autTaskName.taskName, autTaskName);
        }
    }

    AutomatedTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }

    public static AutomatedTaskName valueOfTaskName(String taskName) {
        return BY_TASK_NAME.get(taskName);
    }
}
