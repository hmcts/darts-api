package uk.gov.hmcts.darts.notification.exception.exception;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) {
        super(message);
    }
}
