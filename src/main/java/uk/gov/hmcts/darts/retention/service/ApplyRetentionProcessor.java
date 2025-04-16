package uk.gov.hmcts.darts.retention.service;

@FunctionalInterface
public interface ApplyRetentionProcessor {
    void processApplyRetention(Integer batchSize);
}
