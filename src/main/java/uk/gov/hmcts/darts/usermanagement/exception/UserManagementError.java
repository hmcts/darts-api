package uk.gov.hmcts.darts.usermanagement.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum UserManagementError implements DartsApiError {

    USER_NOT_FOUND(
          "100",
          HttpStatus.NOT_FOUND,
          "The provided user does not exist"
    ),
    DUPLICATE_EMAIL(
          "101",
          HttpStatus.CONFLICT,
          "The provided email already exists"
    ),
    DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED(
          "110",
          HttpStatus.CONFLICT,
          "Duplicate security group names are not permitted"
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
