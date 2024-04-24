package uk.gov.hmcts.darts.arm.component;

public interface ArmRetentionEventDateCalculator {

    boolean calculateRetentionEventDate(Integer externalObjectDirectoryId);

}
