package uk.gov.hmcts.darts.arm.exception;

import uk.gov.hmcts.darts.common.exception.DartsException;

public class ArmRpoException extends DartsException {

    public ArmRpoException(String message) {
        this(message, null);
    }

    public ArmRpoException(String message, Throwable cause) {
        super(message, cause);
    }

}
