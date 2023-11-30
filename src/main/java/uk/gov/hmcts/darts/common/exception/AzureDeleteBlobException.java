package uk.gov.hmcts.darts.common.exception;

public class AzureDeleteBlobException extends Exception {
    public AzureDeleteBlobException(String message) {
        super(message);
    }

    public AzureDeleteBlobException(String message, Throwable cause) {
        super(message, cause);
    }
}
