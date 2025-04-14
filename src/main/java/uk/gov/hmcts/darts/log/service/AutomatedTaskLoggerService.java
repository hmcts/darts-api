package uk.gov.hmcts.darts.log.service;

import java.util.UUID;

public interface AutomatedTaskLoggerService {
    void taskStarted(UUID taskExecutionId, String taskName, Integer batchSize);

    void taskCompleted(UUID taskExecutionId, String taskName);

    void taskFailed(UUID taskExecutionId, String taskName);
}