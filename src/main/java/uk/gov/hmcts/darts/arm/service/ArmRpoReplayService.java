package uk.gov.hmcts.darts.arm.service;

@FunctionalInterface
public interface ArmRpoReplayService {

    void replayArmRpo(int batchSize);
}
