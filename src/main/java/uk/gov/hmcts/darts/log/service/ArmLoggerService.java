package uk.gov.hmcts.darts.log.service;

import java.time.Duration;

@SuppressWarnings("PMD.TooManyMethods") //Logging class with many ARM related logs
public interface ArmLoggerService {

    void armPushSuccessful(Long eodId);

    void armPushFailed(Long eodId);

    void archiveToArmSuccessful(Long eodId);

    void archiveToArmFailed(Long eodId);

    void logArmMissingResponse(Duration armMissingResponseDuration, Long eodId);

    void armRpoSearchSuccessful(Integer executionId);

    void armRpoSearchFailed(Integer executionId);

    void armRpoPollingSuccessful(Integer executionId);

    void armRpoPollingFailed(Integer executionId);
    
    void removeOldArmRpoProductionsSuccessful(Integer executionId);
    
    void removeOldArmRpoProductionsFailed();

    void removeOldArmRpoProductionsFailed(Integer executionId);
}
