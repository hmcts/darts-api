package uk.gov.hmcts.darts.audio.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AudioApiError implements DartsApiError {

    FAILED_TO_PROCESS_AUDIO_REQUEST(
        "100",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to process audio request"
    ),
    REQUESTED_DATA_CANNOT_BE_LOCATED(
        "101",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "The requested data cannot be located"
    ),
    MEDIA_NOT_FOUND(
        "102",
        HttpStatus.NOT_FOUND,
        "The requested media cannot be found"
    ),
    FAILED_TO_UPLOAD_AUDIO_FILE(
        "103",
        HttpStatus.BAD_REQUEST,
        "Failed to store uploaded audio file"
    ),
    MISSING_SYSTEM_USER(
        "104",
        null,
        "Failed to mark audio(s) for deletion as system user was not found"
    );

    private static final String ERROR_TYPE_PREFIX = "AUDIO";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
