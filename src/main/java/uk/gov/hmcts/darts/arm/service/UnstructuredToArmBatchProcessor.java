package uk.gov.hmcts.darts.arm.service;

@FunctionalInterface
public interface UnstructuredToArmBatchProcessor {
    void processUnstructuredToArm(int armBatchSize);
}
