package uk.gov.hmcts.darts.common.exception;

public class DartsException extends RuntimeException {

    public DartsException(String message, Throwable cause) {
        super(message, cause);
    }

    public DartsException(String message) {
        super(message);
    }

    public DartsException(Throwable cause) {
        super(cause);
    }
}
