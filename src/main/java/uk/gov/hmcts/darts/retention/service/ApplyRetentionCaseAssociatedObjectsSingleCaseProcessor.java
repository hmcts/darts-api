package uk.gov.hmcts.darts.retention.service;

@FunctionalInterface
public interface ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor {

    void processApplyRetentionToCaseAssociatedObjects(Integer caseId);
}
