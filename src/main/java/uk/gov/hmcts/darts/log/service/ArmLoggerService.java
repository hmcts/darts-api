package uk.gov.hmcts.darts.log.service;

public interface ArmLoggerService {

    void armPushSuccessful(Integer eodId);

    void armPushFailed(Integer eodId);

    void archiveToArmSuccessful(Integer eodId);

    void archiveToArmFailed(Integer eodId);

    void armRpoSearchSuccessful(Integer executionId);

    void armRpoSearchFailed(Integer executionId);

    void armRpoPollingSuccessful(Integer executionId);

    void armRpoPollingFailed(Integer executionId);
}
