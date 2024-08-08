package uk.gov.hmcts.darts.log.service;

public interface ArmLoggerService {

    void armPushSuccessful(Integer eodId);

    void armPushFailed(Integer eodId);

    void archiveToArmSuccessful(Integer eodId);

    void archiveToArmFailed(Integer eodId);

}
