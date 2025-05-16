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
    ),
    EVENT_MAPPING_DUPLICATE_IN_DB(
        EventErrorCode.DUPLICATE_EVENT_MAPPING.getValue(),
        HttpStatus.CONFLICT,
        EventTitleErrors.DUPLICATE_EVENT_MAPPING.toString(),
        false
    ),
    EVENT_MAPPING_DOES_NOT_EXIST_IN_DB(
        EventErrorCode.NO_EVENT_MAPPING.getValue(),
        HttpStatus.CONFLICT,
        EventTitleErrors.NO_EVENT_MAPPING.toString(),
        false
    ),
    EVENT_HANDLER_NAME_DOES_NOT_EXIST(
        EventErrorCode.INVALID_HANDLER_MAPPING_NAME.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        EventTitleErrors.INVALID_HANDLER_MAPPING_NAME.toString()
    ),
    EVENT_HANDLER_MAPPING_INACTIVE(
        EventErrorCode.MAPPING_INACTIVE.getValue(),
        HttpStatus.CONFLICT,
        EventTitleErrors.MAPPING_INACTIVE.toString(),
        false
    ),
    EVENT_HANDLER_MAPPING_IN_USE(
        EventErrorCode.MAPPING_IN_USE.getValue(),
        HttpStatus.CONFLICT,
        EventTitleErrors.MAPPING_IN_USE.toString(),
        false
    ),
    TOO_MANY_SEARCH_RESULTS(
        EventErrorCode.TOO_MANY_RESULTS.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        EventTitleErrors.TOO_MANY_RESULTS.toString()
    ),
    EVENT_ID_NOT_FOUND_RESULTS(
        EventErrorCode.EVENT_ID_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        EventTitleErrors.EVENT_ID_NOT_FOUND.toString()
    ),
    EVENT_ALREADY_CURRENT(
        EventErrorCode.EVENT_ALREADY_CURRENT.getValue(),
        HttpStatus.CONFLICT,
        EventTitleErrors.EVENT_ALREADY_CURRENT.toString(),
        false
    ),
    CAN_NOT_UPDATE_EVENT_ID_0(
        EventErrorCode.CAN_NOT_UPDATE_EVENT_ID_0.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        EventTitleErrors.CAN_NOT_UPDATE_EVENT_ID_0.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "EVENT";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;
    private final boolean shouldLogException;

    EventError(String errorTypeNumeric, HttpStatus httpStatus, String title) {
        this(errorTypeNumeric, httpStatus, title, true);
    }

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

    @Override
    public boolean shouldLogException() {
        return shouldLogException;
    }

}