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
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.FAILED_TO_UPLOAD_AUDIO_FILE.toString()
    ),
    MISSING_SYSTEM_USER(
        AddAudioErrorCode.MISSING_SYSTEM_USER.getValue(),
        null,
        AddAudioTitleErrors.MISSING_SYSTEM_USER.toString()
    ),
    MEDIA_ALREADY_HIDDEN(
        AddAudioErrorCode.MEDIA_ALREADY_HIDDEN.getValue(),
        HttpStatus.CONFLICT,
        AddAudioTitleErrors.MEDIA_ALREADY_HIDDEN.getValue(),
        false
    ),
    MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE(
        AddAudioErrorCode.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.MEDIA_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getValue()
    ),
    MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE(
        AddAudioErrorCode.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.MEDIA_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getValue()
    ),
    MEDIA_HIDE_ACTION_REASON_NOT_FOUND(
        AddAudioErrorCode.MEDIA_HIDE_ACTION_REASON_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.MEDIA_HIDE_ACTION_REASON_NOT_FOUND.getValue()
    ),
    ADMIN_SEARCH_CRITERIA_NOT_SUITABLE(
        AddAudioErrorCode.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.ADMIN_SEARCH_CRITERIA_NOT_SUITABLE.getValue()
    ),
    TOO_MANY_RESULTS(
        AddAudioErrorCode.TOO_MANY_RESULTS.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.TOO_MANY_RESULTS.getValue()
    ),
    MEDIA_ALREADY_MARKED_FOR_DELETION(
        AddAudioErrorCode.MEDIA_ALREADY_MARKED_FOR_DELETION.getValue(),
        HttpStatus.CONFLICT,
        AddAudioTitleErrors.MEDIA_ALREADY_MARKED_FOR_DELETION.getValue(),
        false
    ),
    ADMIN_MEDIA_MARKED_FOR_DELETION_NOT_FOUND(
        AddAudioErrorCode.ADMIN_MEDIA_MARKED_FOR_DELETION_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        AddAudioTitleErrors.ADMIN_MEDIA_MARKED_FOR_DELETION_NOT_FOUND.getValue()
    ),
    MEDIA_MARKED_FOR_DELETION_REASON_NOT_FOUND(
        AddAudioErrorCode.MARKED_FOR_DELETION_REASON_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.MARKED_FOR_DELETION_REASON_NOT_FOUND.getValue()
    ),
    USER_CANNOT_APPROVE_THEIR_OWN_DELETION(
        AddAudioErrorCode.USER_CANT_APPROVE_THEIR_OWN_DELETION.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.USER_CANT_APPROVE_THEIR_OWN_DELETION.getValue()
    ),
    FAILED_TO_ADD_AUDIO_META_DATA(
        AddAudioErrorCode.FAILED_TO_ADD_AUDIO_META_DATA.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.FAILED_TO_ADD_AUDIO_META_DATA.getValue()
    ),
    START_TIME_END_TIME_NOT_VALID(
        AddAudioErrorCode.START_TIME_END_TIME_NOT_VALID.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AddAudioTitleErrors.START_TIME_END_TIME_NOT_VALID.getValue()
    ),
    MEDIA_ALREADY_CURRENT(
        AddAudioErrorCode.MEDIA_ALREADY_CURRENT.getValue(),
        HttpStatus.CONFLICT,
        AddAudioTitleErrors.MEDIA_ALREADY_CURRENT.getValue(),
        false
    );


    private static final String ERROR_TYPE_PREFIX = "AUDIO";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;
    private final boolean shouldLogException;

    AudioApiError(String errorTypeNumeric, HttpStatus httpStatus, String title) {
        this(errorTypeNumeric, httpStatus, title, true);
    }

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

    @Override
    public boolean shouldLogException() {
        return shouldLogException;
    }

}