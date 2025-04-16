package uk.gov.hmcts.darts.cases.service;

@FunctionalInterface
public interface CaseExpiryDeleter {

    void delete(Integer batchSize);

}
