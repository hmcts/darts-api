package uk.gov.hmcts.darts.datamanagement.service;

@FunctionalInterface
public interface AssociatedObjectDataExpiryDeleterService {

    void delete(Integer batchSize);

}
