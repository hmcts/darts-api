package uk.gov.hmcts.darts.transcriptions.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum TranscriptionApiError implements DartsApiError {
    FAILED_TO_VALIDATE_TRANSCRIPTION_REQUEST(
        "100",
        HttpStatus.BAD_REQUEST,
        "Failed to validate transcription request"
    ),
    TRANSCRIPTION_NOT_FOUND(
        "101",
        HttpStatus.NOT_FOUND,
        "The requested transcription cannot be found"
    ),
    BAD_REQUEST_TRANSCRIPTION_STATUS(
        "102",
        HttpStatus.BAD_REQUEST,
        "Unexpected transcription status for this workflow"
    ),
    BAD_REQUEST_WORKFLOW_COMMENT(
        "103",
        HttpStatus.BAD_REQUEST,
        "The workflow comment is required for this transcription update"
    ),
    TRANSCRIPTION_WORKFLOW_ACTION_INVALID(
        "104",
        HttpStatus.CONFLICT,
        "Transcription workflow action is not permitted"
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
