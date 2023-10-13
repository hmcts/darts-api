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
        HttpStatus.FORBIDDEN,
        "User is not authorised for the associated courthouse"
    ),
    BAD_REQUEST_CASE_ID(
        "101",
        HttpStatus.BAD_REQUEST,
        "Failed to check authorisation for the case"
    ),
    BAD_REQUEST_HEARING_ID(
        "102",
        HttpStatus.BAD_REQUEST,
        "Failed to check authorisation for the hearing"
    ),
    BAD_REQUEST_MEDIA_REQUEST_ID(
        "103",
        HttpStatus.BAD_REQUEST,
        "Failed to check authorisation for the media request"
    ),
    BAD_REQUEST_MEDIA_ID(
        "104",
        HttpStatus.BAD_REQUEST,
        "Failed to check authorisation for the media"
    ),
    BAD_REQUEST_TRANSCRIPTION_ID(
        "105",
        HttpStatus.BAD_REQUEST,
        "Failed to check authorisation for the transcription"
    ),
    USER_DETAILS_INVALID(
        "106",
        HttpStatus.UNAUTHORIZED,
        "Could not obtain user details"
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
