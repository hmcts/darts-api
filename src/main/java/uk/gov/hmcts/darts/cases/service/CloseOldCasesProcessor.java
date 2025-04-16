package uk.gov.hmcts.darts.cases.service;

@FunctionalInterface
public interface CloseOldCasesProcessor {
    void closeCases(int batchSize);
}
