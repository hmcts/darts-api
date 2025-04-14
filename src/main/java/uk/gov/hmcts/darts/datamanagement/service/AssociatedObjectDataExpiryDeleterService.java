package uk.gov.hmcts.darts.datamanagement.service;

public interface AssociatedObjectDataExpiryDeleterService {

    void delete(Integer batchSize);

}
