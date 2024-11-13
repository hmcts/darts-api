package uk.gov.hmcts.darts.retention.service;

public interface ApplyRetentionProcessor {
    void processApplyRetention(Integer batchSize);
}
