package uk.gov.hmcts.darts.event.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum EventError implements DartsApiError {

    EVENT_DATA_NOT_FOUND(
        "100",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Data on the event could not be reconciled with Darts records"
    ),

    EVENT_HANDLER_NOT_FOUND(
        "101",
        HttpStatus.UNPROCESSABLE_ENTITY,
        "No event handler found event"
    );

    private static final String ERROR_TYPE_PREFIX = "EVENT";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
