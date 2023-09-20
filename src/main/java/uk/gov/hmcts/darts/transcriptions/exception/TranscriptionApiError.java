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
