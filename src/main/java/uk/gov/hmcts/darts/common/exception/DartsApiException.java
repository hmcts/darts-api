package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.audio.model.AddAudioTitleErrors;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@SuppressWarnings("PMD.NullAssignment")
public class DartsApiException extends RuntimeException {

    private final DartsApiError error;
    private final String detail;
    private final Map<String, Object> customProperties = new HashMap<>();

    public DartsApiException(DartsApiError error) {
        super(error.getTitle());

        this.error = error;
        this.detail = null;
    }

    public DartsApiException(DartsApiError error, Throwable throwable) {
        super(error.getTitle(), throwable);

        this.error = error;
        this.detail = null;
    }

    public DartsApiException(DartsApiError error, String detail) {
        super(String.format("%s. %s", error.getTitle(), detail));

        this.error = error;
        this.detail = detail;
    }

    public DartsApiException(DartsApiError error, Map<String, Object> customProperties) {
        super(error.getTitle());

        this.error = error;
        this.detail = null;
        this.customProperties.putAll(customProperties);
    }

    public DartsApiException(DartsApiError error, String detail, Map<String, Object> customProperties) {
        super(String.format("%s. %s", error.getTitle(), detail));

        this.error = error;
        this.detail = detail;
        this.customProperties.putAll(customProperties);
    }

    public DartsApiException(DartsApiError error, String detail, Throwable throwable) {
        super(String.format("%s. %s", error.getTitle(), detail), throwable);

        this.error = error;
        this.detail = detail;
    }

    @Getter
    @RequiredArgsConstructor
    public enum DartsApiErrorCommon implements DartsApiError {
        FEATURE_FLAG_NOT_ENABLED(
            "FEATURE_FLAG_NOT_ENABLED",
            HttpStatus.NOT_IMPLEMENTED,
            AddAudioTitleErrors.USER_CANT_APPROVE_THEIR_OWN_DELETION.getValue()
        );
        private static final String ERROR_TYPE_PREFIX = "COMMON";

        private final String errorTypeNumeric;
        private final HttpStatus httpStatus;
        private final String title;

        @Override
        public String getErrorTypePrefix() {
            return ERROR_TYPE_PREFIX;
        }

    }

}
