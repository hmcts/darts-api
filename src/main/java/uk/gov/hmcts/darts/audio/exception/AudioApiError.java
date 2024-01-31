package uk.gov.hmcts.darts.audio.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.audio.model.AddAudioErrorCode;
import uk.gov.hmcts.darts.audio.model.AddAudioTitleErrors;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AudioApiError implements DartsApiError {

    FAILED_TO_PROCESS_AUDIO_REQUEST(
        AddAudioErrorCode.FAILED_TO_PROCESS_AUDIO_REQUEST.getValue(),
        null,
        AddAudioTitleErrors.FAILED_TO_PROCESS_AUDIO_REQUEST.toString()
    ),
    REQUESTED_DATA_CANNOT_BE_LOCATED(
        AddAudioErrorCode.REQUESTED_DATA_CANNOT_BE_LOCATED.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        AddAudioTitleErrors.REQUESTED_DATA_CANNOT_BE_LOCATED.toString()
    ),
    MEDIA_NOT_FOUND(
        AddAudioErrorCode.MEDIA_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        AddAudioTitleErrors.MEDIA_NOT_FOUND.toString()
    ),
    FAILED_TO_UPLOAD_AUDIO_FILE(
        AddAudioErrorCode.FAILED_TO_UPLOAD_AUDIO_FILE.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.FAILED_TO_UPLOAD_AUDIO_FILE.toString()
    ),
    MISSING_SYSTEM_USER(
        AddAudioErrorCode.MISSING_SYSTEM_USER.getValue(),
        null,
        AddAudioTitleErrors.MISSING_SYSTEM_USER.toString()
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
