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
        HttpStatus.INTERNAL_SERVER_ERROR,
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
    ),
    AUDIO_NOT_PROVIDED(
        AddAudioErrorCode.AUDIO_NOT_PROVIDED.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.AUDIO_NOT_PROVIDED.toString()
    ),
    UNEXPECTED_FILE_TYPE(
        AddAudioErrorCode.UNEXPECTED_FILE_TYPE.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.UNEXPECTED_FILE_TYPE.toString()
    ),
    FILE_DURATION_OUT_OF_BOUNDS(
        AddAudioErrorCode.FILE_DURATION_OUT_OF_BOUNDS.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.FILE_DURATION_OUT_OF_BOUNDS.toString()
    ),
    FILE_SIZE_OUT_OF_BOUNDS(
        AddAudioErrorCode.FILE_SIZE_OUT_OF_BOUNDS.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.FILE_SIZE_OUT_OF_BOUNDS.toString()
    ),
    ADMIN_SEARCH_CRITERIA_NOT_PROVIDED(
        AddAudioErrorCode.ADMIN_SEARCH_CRITERIA_NOT_PROVIDED.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.ADMIN_SEARCH_CRITERIA_NOT_PROVIDED.toString()
    ),
    MEDIA_ALREADY_HIDDEN(
        AddAudioErrorCode.MEDIA_ALREADY_HIDDEN.getValue(),
        HttpStatus.CONFLICT,
        AddAudioTitleErrors.MEDIA_ALREADY_HIDDEN.getValue()
    ),
    MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE(
        AddAudioErrorCode.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getValue()
    ),
    MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE(
        AddAudioErrorCode.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioTitleErrors.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getValue()
    ),
    MEDIA_HIDE_ACTION_REASON_NOT_FOUND(
        AddAudioErrorCode.MEDIA_HIDE_ACTION_REASON_NOT_FOUND.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioErrorCode.MEDIA_HIDE_ACTION_REASON_NOT_FOUND.getValue()
    ),
    REASON_IS_MARKED_FOR_DELETION(
        AddAudioErrorCode.MEDIA_HIDE_ACTION_REASON_MARKED_FOR_DELETION.getValue(),
        HttpStatus.NOT_IMPLEMENTED,
        AddAudioErrorCode.MEDIA_HIDE_ACTION_REASON_MARKED_FOR_DELETION.getValue()),
    ADMIN_SEARCH_CRITERIA_NOT_SUITABLE(
        AddAudioErrorCode.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE.getValue(),
        HttpStatus.BAD_REQUEST,
        AddAudioErrorCode.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE.getValue());

    private static final String ERROR_TYPE_PREFIX = "AUDIO";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}