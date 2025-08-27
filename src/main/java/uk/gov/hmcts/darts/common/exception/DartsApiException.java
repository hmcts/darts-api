package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class DartsApiException extends RuntimeException {

    private static final String EXCEPTION_MESSAGE_FORMAT = "%s. %s";

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
        this(error, detail, true);
    }

    public DartsApiException(DartsApiError error, String detail, boolean formatDetail) {
        super(formatDetail ? String.format(EXCEPTION_MESSAGE_FORMAT, error.getTitle(), detail) : detail);
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
        super(String.format(EXCEPTION_MESSAGE_FORMAT, error.getTitle(), detail));

        this.error = error;
        this.detail = detail;
        this.customProperties.putAll(customProperties);
    }

}
