package uk.gov.hmcts.darts.arm.service;


public interface DetsToArmBatchPushProcessor {
    void processDetsToArm(int batchSize);
}