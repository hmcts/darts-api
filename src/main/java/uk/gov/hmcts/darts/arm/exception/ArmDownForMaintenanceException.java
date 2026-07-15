package uk.gov.hmcts.darts.arm.exception;

public class ArmDownForMaintenanceException extends Exception {
    public ArmDownForMaintenanceException(String message) {
        super(message);
    }
}