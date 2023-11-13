package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@SuppressWarnings("PMD.NullAssignment")
public class DartsApiException extends RuntimeException {

    private final DartsApiError error;
    private final String detail;
    private final HashMap<String, String> customProperties = new HashMap<>();

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

    public DartsApiException(DartsApiError error, Map<String, String> customProperties) {
        super(error.getTitle());

        this.error = error;
        this.detail = null;
        this.customProperties.putAll(customProperties);
    }

    public DartsApiException(DartsApiError error, String detail, Map<String, String> customProperties) {
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

}
