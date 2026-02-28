package uk.gov.hmcts.darts.arm.service;

import java.time.Duration;

@FunctionalInterface
public interface ArmRpoBacklogCatchupService {

    void performCatchup(Integer batchSize, Integer maxHoursStartingPoint, Integer totalCatchupHours, Duration threadSleepDuration);

}
