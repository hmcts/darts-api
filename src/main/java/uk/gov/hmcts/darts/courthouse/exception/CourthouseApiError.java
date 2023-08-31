package uk.gov.hmcts.darts.courthouse.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum CourthouseApiError implements DartsApiError {

    COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS(
        "100",
        HttpStatus.CONFLICT,
        "Provided courthouse name already exists."
    ),
    COURTHOUSE_CODE_PROVIDED_ALREADY_EXISTS(
        "101",
        HttpStatus.CONFLICT,
        "Provided courthouse code already exists."
    );

    private static final String ERROR_TYPE_PREFIX = "COURTHOUSE";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
