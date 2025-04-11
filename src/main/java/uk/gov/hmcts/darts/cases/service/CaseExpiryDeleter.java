package uk.gov.hmcts.darts.cases.service;

public interface CaseExpiryDeleter {

    void delete(Integer batchSize);

}
