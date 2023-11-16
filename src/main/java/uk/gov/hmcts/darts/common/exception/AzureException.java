package uk.gov.hmcts.darts.common.exception;

public class AzureException extends RuntimeException {

    public AzureException(String message, Throwable cause) {
        super(message, cause);
    }

}
