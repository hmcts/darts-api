package uk.gov.hmcts.darts.casedocument.service;

@FunctionalInterface
public interface GenerateCaseDocumentProcessor {

    void processGenerateCaseDocument(int batchSize);
}
