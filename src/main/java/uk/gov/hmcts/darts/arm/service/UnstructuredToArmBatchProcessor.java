package uk.gov.hmcts.darts.arm.service;


public interface UnstructuredToArmBatchProcessor {
    void processUnstructuredToArm(int armBatchSize);
}
