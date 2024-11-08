package uk.gov.hmcts.darts.arm.service;

public interface ArmRetentionEventDateProcessor {
    void calculateEventDates(Integer batchSize);
}
