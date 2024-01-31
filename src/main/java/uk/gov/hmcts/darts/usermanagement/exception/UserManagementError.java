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
            HttpStatus.UNPROCESSABLE_ENTITY,
        UserManagementTitleErrors.DUPLICATE_EMAIL.toString()),
    DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED(
        UserManagementErrorCode.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED.getValue(),
        HttpStatus.CONFLICT,
        UserManagementTitleErrors.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED.toString()
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
