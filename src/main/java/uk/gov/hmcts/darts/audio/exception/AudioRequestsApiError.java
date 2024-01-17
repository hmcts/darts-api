package uk.gov.hmcts.darts.audio.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AudioRequestsApiError implements DartsApiError {

    MEDIA_REQUEST_NOT_FOUND(
        "100",
        HttpStatus.NOT_FOUND,
        "The requested audio request cannot be found"
    ),
    MEDIA_REQUEST_NOT_VALID_FOR_USER(
        "101",
        HttpStatus.FORBIDDEN,
        "The audio request is not valid for this user"
    ),
    MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT(
        "102",
        HttpStatus.BAD_REQUEST,
        "The audio request is not valid for this action"
    ),
    TRANSFORMED_MEDIA_NOT_FOUND(
        "103",
        HttpStatus.NOT_FOUND,
        "The requested transformed media cannot be found"
    ),
    DUPLICATE_MEDIA_REQUEST(
        "104",
        HttpStatus.CONFLICT,
        "An audio request already exists with these properties"
    );

    private static final String ERROR_TYPE_PREFIX = "AUDIO_REQUESTS";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
