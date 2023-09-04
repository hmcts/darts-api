package uk.gov.hmcts.darts.authorisation.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AuthorisationError implements DartsApiError {

    USER_NOT_AUTHORISED_FOR_COURTHOUSE(
        "100",
        HttpStatus.UNAUTHORIZED,
        "User is not authorised for the associated courthouse"
    ),
    BAD_CASE_ID_REQUEST(
        "101",
        HttpStatus.BAD_REQUEST,
        "Failed to check authorisation for the case"
    );

    private static final String ERROR_TYPE_PREFIX = "AUTHORISATION";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
