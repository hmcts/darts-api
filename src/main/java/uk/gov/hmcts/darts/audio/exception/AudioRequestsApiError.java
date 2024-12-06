package uk.gov.hmcts.darts.audio.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestsErrorCode;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestsTitleErrors;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AudioRequestsApiError implements DartsApiError {

    MEDIA_REQUEST_NOT_FOUND(
        AudioRequestsErrorCode.MEDIA_REQUEST_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        AudioRequestsTitleErrors.MEDIA_REQUEST_NOT_FOUND.toString()
    ),
    MEDIA_REQUEST_NOT_VALID_FOR_USER(
        AudioRequestsErrorCode.MEDIA_REQUEST_NOT_VALID_FOR_USER.getValue(),
        HttpStatus.FORBIDDEN,
        AudioRequestsTitleErrors.MEDIA_REQUEST_NOT_VALID_FOR_USER.toString()
    ),
    MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT(
        AudioRequestsErrorCode.MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT.getValue(),
        HttpStatus.BAD_REQUEST,
        AudioRequestsTitleErrors.MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT.toString()
    ),
    TRANSFORMED_MEDIA_NOT_FOUND(
        AudioRequestsErrorCode.TRANSFORMED_MEDIA_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        AudioRequestsTitleErrors.TRANSFORMED_MEDIA_NOT_FOUND.toString()
    ),
    DUPLICATE_MEDIA_REQUEST(
        AudioRequestsErrorCode.DUPLICATE_MEDIA_REQUEST.getValue(),
        HttpStatus.CONFLICT,
        AudioRequestsTitleErrors.DUPLICATE_MEDIA_REQUEST.toString()
    ),
    USER_IS_NOT_FOUND(
        AudioRequestsErrorCode.USER_NOT_FOUND.getValue(),
        HttpStatus.BAD_REQUEST,
        AudioRequestsTitleErrors.USER_NOT_FOUND.toString()
    ),
    INVALID_REQUEST(
        AudioRequestsErrorCode.INVALID_REQUEST.getValue(),
        HttpStatus.BAD_REQUEST,
        AudioRequestsTitleErrors.INVALID_REQUEST.toString()
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