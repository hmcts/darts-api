package uk.gov.hmcts.darts.audio.exception;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum ExternalDataDeleterException implements DartsApiError {

    MISSING_SYSTEM_USER(
        "100",
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
