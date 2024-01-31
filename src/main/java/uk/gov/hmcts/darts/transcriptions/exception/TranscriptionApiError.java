package uk.gov.hmcts.darts.transcriptions.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionAuthorisation403ErrorCode;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionsErrorCode;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionsTitleErrors;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscription400ErrorCode;
import uk.gov.hmcts.darts.transcriptions.model.UpdateTranscriptions400ErrorCode;

@Getter
@RequiredArgsConstructor
public enum TranscriptionApiError implements DartsApiError {
    FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST(
        TranscriptionsErrorCode.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST.getValue(),
        HttpStatus.BAD_REQUEST,
        TranscriptionsTitleErrors.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST.toString()
    ),
    TRANSCRIPTION_NOT_FOUND(
        TranscriptionsErrorCode.TRANSCRIPTION_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        TranscriptionsTitleErrors.TRANSCRIPTION_NOT_FOUND.toString()
    ),
    BAD_REQUEST_TRANSCRIPTION_STATUS(
        TranscriptionsErrorCode.BAD_REQUEST_TRANSCRIPTION_STATUS.getValue(),
        HttpStatus.BAD_REQUEST,
        TranscriptionsTitleErrors.BAD_REQUEST_TRANSCRIPTION_STATUS.toString()
    ),
    BAD_REQUEST_WORKFLOW_COMMENT(
        TranscriptionsErrorCode.BAD_REQUEST_WORKFLOW_COMMENT.getValue(),
        HttpStatus.BAD_REQUEST,
        TranscriptionsTitleErrors.BAD_REQUEST_WORKFLOW_COMMENT.toString()
    ),
    BAD_REQUEST_TRANSCRIPTION_TYPE(
        TranscriptionsErrorCode.BAD_REQUEST_TRANSCRIPTION_TYPE.getValue(),
        HttpStatus.BAD_REQUEST,
        TranscriptionsTitleErrors.BAD_REQUEST_TRANSCRIPTION_TYPE.toString()
    ),
    TRANSCRIPTION_WORKFLOW_ACTION_INVALID(
        TranscriptionsErrorCode.TRANSCRIPTION_WORKFLOW_ACTION_INVALID.getValue(),
        HttpStatus.CONFLICT,
        TranscriptionsTitleErrors.TRANSCRIPTION_WORKFLOW_ACTION_INVALID.toString()
    ),
    BAD_REQUEST_TRANSCRIPTION_URGENCY(
        TranscriptionsErrorCode.BAD_REQUEST_TRANSCRIPTION_URGENCY.getValue(),
        HttpStatus.BAD_REQUEST,
        TranscriptionsTitleErrors.BAD_REQUEST_TRANSCRIPTION_URGENCY.toString()
    ),
    DUPLICATE_TRANSCRIPTION(
        TranscriptionsErrorCode.DUPLICATE_TRANSCRIPTION.getValue(),
        HttpStatus.CONFLICT,
        TranscriptionsTitleErrors.DUPLICATE_TRANSCRIPTION.toString()
    ),
    FAILED_TO_ATTACH_TRANSCRIPT(
        TranscriptionsErrorCode.FAILED_TO_ATTACH_TRANSCRIPT.getValue(),
        HttpStatus.BAD_REQUEST,
        TranscriptionsTitleErrors.FAILED_TO_ATTACH_TRANSCRIPT.toString()
    ),
    FAILED_TO_DOWNLOAD_TRANSCRIPT(
        TranscriptionsErrorCode.FAILED_TO_DOWNLOAD_TRANSCRIPT.getValue(),
        HttpStatus.BAD_REQUEST,
        TranscriptionsTitleErrors.FAILED_TO_DOWNLOAD_TRANSCRIPT.toString()
    ),
    AUDIO_NOT_FOUND(
        TranscriptionsErrorCode.AUDIO_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        TranscriptionsTitleErrors.AUDIO_NOT_FOUND.toString()
    ),
    TIMES_OUTSIDE_OF_HEARING_TIMES(
        TranscriptionsErrorCode.TIMES_OUTSIDE_OF_HEARING_TIMES.getValue(),
        HttpStatus.NOT_FOUND,
        TranscriptionsTitleErrors.TIMES_OUTSIDE_OF_HEARING_TIMES.toString()
    ),
    FAILED_TO_UPDATE_TRANSCRIPTIONS(
        UpdateTranscriptions400ErrorCode.UPDATE_TRANSCRIPTIONS_PARTIAL_PROBLEM.getValue(),
        HttpStatus.BAD_REQUEST,
        "Failed to update some of the transcriptions"
    ),
    USER_NOT_TRANSCRIBER(
        TranscriptionAuthorisation403ErrorCode.USER_NOT_TRANSCRIBER.getValue(),
        HttpStatus.FORBIDDEN,
        "User is not a transcriber user"
    ),
    BAD_REQUEST_TRANSCRIPTION_REQUESTER_IS_SAME_AS_APPROVER(
        UpdateTranscription400ErrorCode.REQUESTER_CANNOT_BE_APPROVER_OR_REJECTER.getValue(),
        HttpStatus.BAD_REQUEST,
        "Transcription requestor cannot approve or reject their own transcription requests."
    );


    private static final String ERROR_TYPE_PREFIX = "TRANSCRIPTION";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
