package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.task.config.AsyncTaskConfig;

public interface ArmResponseFilesProcessor {
    void processResponseFiles(int batchSize, AsyncTaskConfig asyncTaskConfig);
}
