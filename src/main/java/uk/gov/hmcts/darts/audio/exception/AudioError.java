package uk.gov.hmcts.darts.audio.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AudioError implements DartsApiError {

    FAILED_TO_PROCESS_AUDIO_REQUEST("100",
                                  null,
                                  "Failed to process audio request"),
    REQUESTED_DATA_CANNOT_BE_LOCATED("101",
                                     HttpStatus.INTERNAL_SERVER_ERROR,
                                    "The requested data cannot be located");

    private static final String ERROR_TYPE_PREFIX = "AUDIO";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
