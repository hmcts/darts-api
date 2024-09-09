package uk.gov.hmcts.darts.cases.service;

public interface CloseOldCasesProcessor {
    void closeCases(int batchSize);
}
