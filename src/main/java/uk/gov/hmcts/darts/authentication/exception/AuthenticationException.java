package uk.gov.hmcts.darts.authentication.exception;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(String message, String cause) {
        super(String.format("%s: %s", message, cause));
    }

}
