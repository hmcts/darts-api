package uk.gov.hmcts.darts.arm.service;

import java.time.Duration;

@FunctionalInterface
public interface RemoveRpoProductionsService {
    
    void removeOldArmRpoProductions(boolean isManualRun, Duration duration, int batchSize);
    
}
