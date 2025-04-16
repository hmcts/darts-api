package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.task.config.AsyncTaskConfig;

@FunctionalInterface
public interface ArmResponseFilesProcessor {
    void processResponseFiles(int batchSize, AsyncTaskConfig asyncTaskConfig);
}
