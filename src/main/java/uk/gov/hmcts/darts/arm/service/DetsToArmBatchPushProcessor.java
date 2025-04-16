package uk.gov.hmcts.darts.arm.service;

@FunctionalInterface
public interface DetsToArmBatchPushProcessor {
    void processDetsToArm(int batchSize);
}