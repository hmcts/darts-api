package uk.gov.hmcts.darts.arm.service;

import java.time.Duration;

@FunctionalInterface
public interface CleanupDetsDataService {

    void cleanupDetsData(int batchsize, Duration durationInArmStorage);

}
