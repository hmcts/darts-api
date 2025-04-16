package uk.gov.hmcts.darts.arm.service;

@FunctionalInterface
public interface ArmRetentionEventDateProcessor {
    void calculateEventDates(Integer batchSize);
}
