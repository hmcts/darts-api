package uk.gov.hmcts.darts.usermanagement.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.usermanagement.model.UserManagementErrorCode;
import uk.gov.hmcts.darts.usermanagement.model.UserManagementTitleErrors;

@Getter
@RequiredArgsConstructor
public enum UserManagementError implements DartsApiError {

    USER_NOT_FOUND(
        UserManagementErrorCode.USER_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        UserManagementTitleErrors.USER_NOT_FOUND.toString()
    ),
    DUPLICATE_EMAIL(
        UserManagementErrorCode.DUPLICATE_EMAIL.getValue(),
        HttpStatus.CONFLICT,
        UserManagementTitleErrors.DUPLICATE_EMAIL.toString()
    ),
    DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED(
        UserManagementErrorCode.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED.getValue(),
        HttpStatus.CONFLICT,
        UserManagementTitleErrors.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED.toString()
    ),
    DUPLICATE_SECURITY_GROUP_DISPLAY_NAME_NOT_PERMITTED(
        UserManagementErrorCode.DUPLICATE_SECURITY_GROUP_DISPLAY_NAME_NOT_PERMITTED.getValue(),
        HttpStatus.CONFLICT,
        UserManagementTitleErrors.DUPLICATE_SECURITY_GROUP_DISPLAY_NAME_NOT_PERMITTED.toString()
    ),
    INVALID_EMAIL_FORMAT(
        UserManagementErrorCode.INVALID_EMAIL_FORMAT.getValue(),
        HttpStatus.BAD_REQUEST,
        UserManagementTitleErrors.INVALID_EMAIL_FORMAT.toString()
    ),
    SECURITY_GROUP_NOT_FOUND(
        UserManagementErrorCode.SECURITY_GROUP_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        UserManagementTitleErrors.SECURITY_GROUP_NOT_FOUND.toString()
    ),
    SECURITY_GROUP_NOT_ALLOWED(
        UserManagementErrorCode.SECURITY_GROUP_NOT_ALLOWED.getValue(),
        HttpStatus.BAD_REQUEST,
        UserManagementTitleErrors.SECURITY_GROUP_NOT_ALLOWED.toString()
    ),
    COURTHOUSE_NOT_FOUND(
        UserManagementErrorCode.COURTHOUSE_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        UserManagementTitleErrors.COURTHOUSE_NOT_FOUND.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "USER_MANAGEMENT";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
