package uk.gov.hmcts.darts.casedocument.service;

@FunctionalInterface
public interface GenerateCaseDocumentForRetentionDateProcessor {

    void processGenerateCaseDocumentForRetentionDate(int batchSize);
}
