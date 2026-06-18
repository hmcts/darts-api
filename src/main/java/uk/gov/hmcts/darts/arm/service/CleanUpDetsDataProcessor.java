package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;

@FunctionalInterface
public interface CleanUpDetsDataProcessor {
    void processCleanUpDetsData(int batchSize, CleanUpDetsDataAutomatedTaskConfig minimumStoredAge);
}
