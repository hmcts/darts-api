package uk.gov.hmcts.darts.arm.component;

@FunctionalInterface
public interface ArmRetentionEventDateCalculator {
    boolean calculateRetentionEventDate(Integer externalObjectDirectoryId);
}
