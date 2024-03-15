package uk.gov.hmcts.darts.authentication.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.common.model.AuthenticationErrorCode;
import uk.gov.hmcts.darts.common.model.AuthenticationTitleErrors;

@Getter
@RequiredArgsConstructor
public enum AuthenticationError implements DartsApiError {

    FAILED_TO_OBTAIN_ACCESS_TOKEN(
        AuthenticationErrorCode.FAILED_TO_OBTAIN_ACCESS_TOKEN.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        AuthenticationTitleErrors.FAILED_TO_OBTAIN_ACCESS_TOKEN.toString()
    ),

    FAILED_TO_VALIDATE_ACCESS_TOKEN(
        AuthenticationErrorCode.FAILED_TO_VALIDATE_ACCESS_TOKEN.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        AuthenticationTitleErrors.FAILED_TO_VALIDATE_ACCESS_TOKEN.toString()
    ),

    FAILED_TO_PARSE_ACCESS_TOKEN(
        AuthenticationErrorCode.FAILED_TO_PARSE_ACCESS_TOKEN.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        AuthenticationTitleErrors.FAILED_TO_PARSE_ACCESS_TOKEN.toString()
    ),

    FAILED_TO_OBTAIN_AUTHENTICATION_CONFIG(AuthenticationErrorCode.FAILED_TO_OBTAIN_ACCESS_TOKEN.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        AuthenticationTitleErrors.FAILED_TO_OBTAIN_AUTHENTICATION_CONFIG.toString());

    private static final String ERROR_TYPE_PREFIX = "AUTHENTICATION";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
