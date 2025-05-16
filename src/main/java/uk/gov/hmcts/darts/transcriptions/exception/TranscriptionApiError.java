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
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST.toString()
    ),
    TRANSCRIPTION_NOT_FOUND(
        TranscriptionsErrorCode.TRANSCRIPTION_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        TranscriptionsTitleErrors.TRANSCRIPTION_NOT_FOUND.toString()
    ),
    BAD_REQUEST_TRANSCRIPTION_STATUS(
        TranscriptionsErrorCode.BAD_REQUEST_TRANSCRIPTION_STATUS.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.BAD_REQUEST_TRANSCRIPTION_STATUS.toString()
    ),
    BAD_REQUEST_WORKFLOW_COMMENT(
        TranscriptionsErrorCode.BAD_REQUEST_WORKFLOW_COMMENT.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.BAD_REQUEST_WORKFLOW_COMMENT.toString()
    ),
    BAD_REQUEST_TRANSCRIPTION_TYPE(
        TranscriptionsErrorCode.BAD_REQUEST_TRANSCRIPTION_TYPE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.BAD_REQUEST_TRANSCRIPTION_TYPE.toString()
    ),
    TRANSCRIPTION_WORKFLOW_ACTION_INVALID(
        TranscriptionsErrorCode.TRANSCRIPTION_WORKFLOW_ACTION_INVALID.getValue(),
        HttpStatus.CONFLICT,
        TranscriptionsTitleErrors.TRANSCRIPTION_WORKFLOW_ACTION_INVALID.toString(),
        false
    ),
    BAD_REQUEST_TRANSCRIPTION_URGENCY(
        TranscriptionsErrorCode.BAD_REQUEST_TRANSCRIPTION_URGENCY.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.BAD_REQUEST_TRANSCRIPTION_URGENCY.toString()
    ),
    DUPLICATE_TRANSCRIPTION(
        TranscriptionsErrorCode.DUPLICATE_TRANSCRIPTION.getValue(),
        HttpStatus.CONFLICT,
        TranscriptionsTitleErrors.DUPLICATE_TRANSCRIPTION.toString(),
        false
    ),
    FAILED_TO_ATTACH_TRANSCRIPT(
        TranscriptionsErrorCode.FAILED_TO_ATTACH_TRANSCRIPT.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.FAILED_TO_ATTACH_TRANSCRIPT.toString()
    ),
    FAILED_TO_UPLOAD_TRANSCRIPT(
        TranscriptionsErrorCode.FAILED_TO_UPLOAD_TRANSCRIPT.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        TranscriptionsTitleErrors.FAILED_TO_UPLOAD_TRANSCRIPT.toString()
    ),
    AUDIO_NOT_FOUND(
        TranscriptionsErrorCode.AUDIO_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        TranscriptionsTitleErrors.AUDIO_NOT_FOUND.toString()
    ),
    FAILED_TO_UPDATE_TRANSCRIPTIONS(
        UpdateTranscriptions400ErrorCode.UPDATE_TRANSCRIPTIONS_PARTIAL_PROBLEM.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Failed to update some of the transcriptions"
    ),
    USER_NOT_TRANSCRIBER(
        TranscriptionAuthorisation403ErrorCode.USER_NOT_TRANSCRIBER.getValue(),
        HttpStatus.FORBIDDEN,
        "User is not a transcriber user"
    ),
    BAD_REQUEST_TRANSCRIPTION_REQUESTER_IS_SAME_AS_APPROVER(
        UpdateTranscription400ErrorCode.REQUESTER_CANNOT_BE_APPROVER_OR_REJECTER.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Transcription requestor cannot approve or reject their own transcription requests."
    ),
    TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_NOT_FOUND.getValue()
    ),
    TRANSCRIPTION_ALREADY_HIDDEN(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_ALREADY_HIDDEN.getValue(),
        HttpStatus.CONFLICT,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_ALREADY_HIDDEN.getValue(),
        false
    ),
    TRANSCRIPTION_DOCUMENT_DELETE_NOT_SUPPORTED(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_DELETE_NOT_SUPPORTED.getValue(),
        HttpStatus.CONFLICT,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_DELETE_NOT_SUPPORTED.getValue(),
        false
    ),
    TRANSCRIPTION_DOCUMENT_DELETION_ALREADY_APPROVED(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_DELETION_ALREADY_APPROVED.getValue(),
        HttpStatus.CONFLICT,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_DELETION_ALREADY_APPROVED.getValue(),
        false
    ),
    TRANSCRIPTION_DOCUMENT_DELETION_CAN_NOT_APPROVE_OWN_REQUEST(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_DELETION_CAN_NOT_APPROVE_OWN_REQUEST.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_DELETION_CAN_NOT_APPROVE_OWN_REQUEST.getValue()
    ),
    TRANSCRIPTION_DOCUMENT_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_PAYLOAD_INCORRECT_USAGE.getValue()
    ),
    TRANSCRIPTION_DOCUMENT_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_SHOW_ACTION_PAYLOAD_INCORRECT_USAGE.getValue()),
    TRANSCRIPTION_DOCUMENT_HIDE_ACTION_REASON_NOT_FOUND(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_REASON_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_HIDE_ACTION_REASON_NOT_FOUND.getValue()
    ),
    TRANSCRIPTION_DOCUMENT_MARKED_FOR_DELETION_REASON_NOT_FOUND(
        TranscriptionsErrorCode.TRANSCRIPTION_DOCUMENT_MARKED_FOR_DELETION_REASON_NOT_FOUND.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.TRANSCRIPTION_DOCUMENT_MARKED_FOR_DELETION_REASON_NOT_FOUND.getValue()
    ),
    TOO_MANY_RESULTS(
        TranscriptionsErrorCode.TOO_MANY_RESULTS.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        TranscriptionsTitleErrors.TOO_MANY_RESULTS.getValue());


    private static final String ERROR_TYPE_PREFIX = "TRANSCRIPTION";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;
    private final boolean shouldLogException;

    TranscriptionApiError(String errorTypeNumeric, HttpStatus httpStatus, String title) {
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