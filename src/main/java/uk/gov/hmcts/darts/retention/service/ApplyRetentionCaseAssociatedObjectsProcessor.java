package uk.gov.hmcts.darts.retention.service;

public interface ApplyRetentionCaseAssociatedObjectsProcessor {

    void processApplyRetentionToCaseAssociatedObjects(Integer batchSize);
}
