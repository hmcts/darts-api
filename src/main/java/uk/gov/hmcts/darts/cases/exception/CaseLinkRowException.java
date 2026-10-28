package uk.gov.hmcts.darts.cases.exception;

import lombok.Getter;

@Getter
public class CaseLinkRowException extends RuntimeException {

    private final String errorCode;

    public CaseLinkRowException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public CaseLinkRowException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
