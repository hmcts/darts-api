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
        UserManagementTitleErrors.DUPLICATE_EMAIL.toString(),
        false
    ),
    DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED(
        UserManagementErrorCode.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED.getValue(),
        HttpStatus.CONFLICT,
        UserManagementTitleErrors.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED.toString(),
        false
    ),
    DUPLICATE_SECURITY_GROUP_DISPLAY_NAME_NOT_PERMITTED(
        UserManagementErrorCode.DUPLICATE_SECURITY_GROUP_DISPLAY_NAME_NOT_PERMITTED.getValue(),
        HttpStatus.CONFLICT,
        UserManagementTitleErrors.DUPLICATE_SECURITY_GROUP_DISPLAY_NAME_NOT_PERMITTED.toString(),
        false
    ),
    INVALID_EMAIL_FORMAT(
        UserManagementErrorCode.INVALID_EMAIL_FORMAT.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        UserManagementTitleErrors.INVALID_EMAIL_FORMAT.toString()
    ),
    SECURITY_GROUP_NOT_FOUND(
        UserManagementErrorCode.SECURITY_GROUP_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        UserManagementTitleErrors.SECURITY_GROUP_NOT_FOUND.toString()
    ),
    SECURITY_GROUP_NOT_ALLOWED(
        UserManagementErrorCode.SECURITY_GROUP_NOT_ALLOWED.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        UserManagementTitleErrors.SECURITY_GROUP_NOT_ALLOWED.toString()
    ),
    COURTHOUSE_NOT_FOUND(
        UserManagementErrorCode.COURTHOUSE_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        UserManagementTitleErrors.COURTHOUSE_NOT_FOUND.toString()
    ),
    USER_ACTIVATION_EMAIL_VIOLATION(
        UserManagementErrorCode.USER_ACTIVATION_EMAIL_VIOLATION.getValue(),
        HttpStatus.CONFLICT,
        UserManagementTitleErrors.USER_ACTIVATION_EMAIL_VIOLATION.toString(),
        false
    );

    private static final String ERROR_TYPE_PREFIX = "USER_MANAGEMENT";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;
    private final boolean logException;

    UserManagementError(String errorTypeNumeric, HttpStatus httpStatus, String title) {
        this(errorTypeNumeric, httpStatus, title, true);
    }

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

    @Override
    public boolean shouldLogException() {
        return logException;
    }

}