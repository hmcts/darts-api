package uk.gov.hmcts.darts.retention.service;

@FunctionalInterface
public interface ResetRetentionAfterDocumentChangeService {

    void updateRetentionAfterDocumentChange(int batchSize);
}
