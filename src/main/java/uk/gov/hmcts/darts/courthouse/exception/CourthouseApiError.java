package uk.gov.hmcts.darts.courthouse.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.courthouse.model.CourthouseErrorCode;
import uk.gov.hmcts.darts.courthouse.model.CourthouseTitleErrors;

@Getter
@RequiredArgsConstructor
public enum CourthouseApiError implements DartsApiError {

    COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS(
        CourthouseErrorCode.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS.getValue(),
        HttpStatus.CONFLICT,
        CourthouseTitleErrors.COURTHOUSE_NAME_PROVIDED_ALREADY_EXISTS.toString()
    ),
    COURTHOUSE_CODE_PROVIDED_ALREADY_EXISTS(
        CourthouseErrorCode.COURTHOUSE_CODE_PROVIDED_ALREADY_EXISTS.getValue(),
        HttpStatus.CONFLICT,
        CourthouseTitleErrors.COURTHOUSE_CODE_PROVIDED_ALREADY_EXISTS.toString()
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
