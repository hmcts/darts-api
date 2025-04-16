package uk.gov.hmcts.darts.retention.service;

@FunctionalInterface
public interface ApplyRetentionCaseAssociatedObjectsProcessor {

    void processApplyRetentionToCaseAssociatedObjects(Integer batchSize);
}
