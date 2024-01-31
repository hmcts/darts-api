package uk.gov.hmcts.darts.event.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.event.model.EventErrorCode;
import uk.gov.hmcts.darts.event.model.EventTitleErrors;

@Getter
@RequiredArgsConstructor
public enum EventError implements DartsApiError {

    EVENT_DATA_NOT_FOUND(
        EventErrorCode.EVENT_DATA_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        EventTitleErrors.EVENT_DATA_NOT_FOUND.toString()
    ),
    EVENT_HANDLER_NOT_FOUND_IN_DB(
        EventErrorCode.EVENT_HANDLER_NOT_FOUND_IN_DB.getValue(),
        HttpStatus.NOT_FOUND,
        EventTitleErrors.EVENT_HANDLER_NOT_FOUND_IN_DB.toString()
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
