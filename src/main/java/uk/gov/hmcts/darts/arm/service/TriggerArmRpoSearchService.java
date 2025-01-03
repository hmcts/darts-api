package uk.gov.hmcts.darts.arm.service;

import java.time.Duration;

public interface TriggerArmRpoSearchService {
    void triggerArmRpoSearch(Duration threadSleepDuration);
}
