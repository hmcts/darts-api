package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonApiError implements DartsApiError {

    COURTHOUSE_PROVIDED_DOES_NOT_EXIST(
        "100",
        HttpStatus.BAD_REQUEST,
        "Provided courthouse does not exist"
    );

    private static final String ERROR_TYPE_PREFIX = "SERVICE";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
