package uk.gov.hmcts.darts.common.exception;

import org.springframework.http.HttpStatus;

public interface DartsApiError {

    String getErrorTypePrefix();

    String getErrorTypeNumeric();

    HttpStatus getHttpStatus();

    String getTitle();

    default String getType() {
        return getErrorTypeNumeric();
    }
}
