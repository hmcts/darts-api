package uk.gov.hmcts.darts.common.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

public interface DartsApiError {

    String getErrorTypePrefix();

    String getErrorTypeNumeric();

    HttpStatus getHttpStatus();

    String getTitle();

    default URI getType() {
        return URI.create(
            getErrorTypePrefix());
    }

}
