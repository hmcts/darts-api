package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuditApiError implements DartsApiError {

    FILTERS_WERE_EMPTY(
        "100",
        HttpStatus.BAD_REQUEST,
        "All filters were empty."
    ),
    DATE_EMPTY(
        "101",
        HttpStatus.BAD_REQUEST,
        "When using date filters, both must be provided."
    ),
    NO_HEARING_OR_USER_FOUND_WHEN_ADDING_AUDIO_AUDIT(
        "101",
        HttpStatus.BAD_REQUEST,
        "No hearing or user found when auditing add audio."
      
        "102",
        HttpStatus.BAD_REQUEST,
        "No hearing or user found when auditing 'add audio' request."
    );

    private static final String ERROR_TYPE_PREFIX = "AUDIT";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
