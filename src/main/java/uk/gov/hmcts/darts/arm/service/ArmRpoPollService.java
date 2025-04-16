package uk.gov.hmcts.darts.arm.service;

import java.time.Duration;

@FunctionalInterface
public interface ArmRpoPollService {

    void pollArmRpo(boolean isManualRun, Duration pollDuration, int batchSize);

}
