package uk.gov.hmcts.darts.authorisation.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.common.model.AuthorisationErrorCode;
import uk.gov.hmcts.darts.common.model.AuthorisationTitleErrors;

@Getter
@RequiredArgsConstructor
public enum AuthorisationError implements DartsApiError {

    USER_NOT_AUTHORISED_FOR_COURTHOUSE(
        AuthorisationErrorCode.USER_NOT_AUTHORISED_FOR_COURTHOUSE.getValue(),
        HttpStatus.FORBIDDEN,
        AuthorisationTitleErrors.USER_NOT_AUTHORISED_FOR_COURTHOUSE.toString()
    ),
    BAD_REQUEST_CASE_ID(
        AuthorisationErrorCode.BAD_REQUEST_CASE_ID.getValue(),
        HttpStatus.BAD_REQUEST,
        AuthorisationTitleErrors.BAD_REQUEST_CASE_ID.toString()
    ),
    BAD_REQUEST_HEARING_ID(
        AuthorisationErrorCode.BAD_REQUEST_HEARING_ID.getValue(),
        HttpStatus.BAD_REQUEST,
        AuthorisationTitleErrors.BAD_REQUEST_HEARING_ID.toString()
    ),
    BAD_REQUEST_MEDIA_REQUEST_ID(
        AuthorisationErrorCode.BAD_REQUEST_MEDIA_REQUEST_ID.getValue(),
        HttpStatus.BAD_REQUEST,
        AuthorisationTitleErrors.BAD_REQUEST_MEDIA_REQUEST_ID.toString()
    ),
    BAD_REQUEST_MEDIA_ID(
        AuthorisationErrorCode.BAD_REQUEST_MEDIA_ID.getValue(),
        HttpStatus.BAD_REQUEST,
        AuthorisationTitleErrors.BAD_REQUEST_MEDIA_ID.toString()
    ),
    BAD_REQUEST_TRANSCRIPTION_ID(
        AuthorisationErrorCode.BAD_REQUEST_TRANSCRIPTION_ID.getValue(),
        HttpStatus.BAD_REQUEST,
        AuthorisationTitleErrors.BAD_REQUEST_TRANSCRIPTION_ID.toString()
    ),
    USER_DETAILS_INVALID(
        AuthorisationErrorCode.USER_DETAILS_INVALID.getValue(),
        HttpStatus.FORBIDDEN,
        AuthorisationTitleErrors.USER_DETAILS_INVALID.toString()
    ),
    BAD_REQUEST_ANY_ID(
        AuthorisationErrorCode.BAD_REQUEST_ANY_ID.getValue(),
        HttpStatus.FORBIDDEN,
        AuthorisationTitleErrors.BAD_REQUEST_ANY_ID.toString()
    ),
    BAD_REQUEST_TRANSFORMED_MEDIA_ID(
        AuthorisationErrorCode.BAD_REQUEST_TRANSFORMED_MEDIA_ID.getValue(),
        HttpStatus.BAD_REQUEST,
        AuthorisationTitleErrors.BAD_REQUEST_TRANSFORMED_MEDIA_ID.toString()
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
