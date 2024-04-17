package uk.gov.hmcts.darts.retention.service;

public interface ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor {

    void processApplyRetentionToCaseAssociatedObjects(Integer caseId);
}
