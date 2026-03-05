package uk.gov.hmcts.darts.retention.service;

import java.time.Duration;

@FunctionalInterface
public interface ApplyRetentionProcessor {

    void processApplyRetention(Integer batchSize, Duration daysBetweenEvents);

}
