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
    ),
    COURTHOUSE_NOT_FOUND(
        CourthouseErrorCode.COURTHOUSE_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        CourthouseTitleErrors.COURTHOUSE_NOT_FOUND.toString()
    ),
    ONLY_TRANSCRIBER_ROLES_MAY_BE_ASSIGNED(
        CourthouseErrorCode.ONLY_TRANSCRIBER_ROLES_MAY_BE_ASSIGNED.getValue(),
        HttpStatus.BAD_REQUEST,
        CourthouseTitleErrors.ONLY_TRANSCRIBER_ROLES_MAY_BE_ASSIGNED.toString()
    ),
    REGION_ID_DOES_NOT_EXIST(
        CourthouseErrorCode.REGION_ID_DOES_NOT_EXIST.getValue(),
        HttpStatus.BAD_REQUEST,
        CourthouseTitleErrors.REGION_ID_DOES_NOT_EXIST.toString()
    ),
    COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS(
        CourthouseErrorCode.COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS.getValue(),
        HttpStatus.CONFLICT,
        CourthouseTitleErrors.COURTHOUSE_DISPLAY_NAME_PROVIDED_ALREADY_EXISTS.toString()
    ),
    SECURITY_GROUP_ID_DOES_NOT_EXIST(
        CourthouseErrorCode.SECURITY_GROUP_ID_DOES_NOT_EXIST.getValue(),
        HttpStatus.BAD_REQUEST,
        CourthouseTitleErrors.SECURITY_GROUP_ID_DOES_NOT_EXIST.toString()
    ),
    COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING(
        CourthouseErrorCode.COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        CourthouseTitleErrors.COURTHOUSE_NAME_CANNOT_BE_CHANGED_CASES_EXISTING.toString()
    ),
    INVALID_REQUEST(
        CourthouseErrorCode.INVALID_REQUEST.getValue(),
        HttpStatus.BAD_REQUEST,
        CourthouseTitleErrors.INVALID_REQUEST.toString()
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
