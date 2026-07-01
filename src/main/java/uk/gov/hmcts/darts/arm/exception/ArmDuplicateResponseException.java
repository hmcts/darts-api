package uk.gov.hmcts.darts.arm.exception;

import uk.gov.hmcts.darts.common.exception.DartsException;

public class ArmDuplicateResponseException extends DartsException {

    public ArmDuplicateResponseException(String message) {
        super(message);
    }

    public ArmDuplicateResponseException(String message, Throwable cause) {
        super(message, cause);
    }

}
