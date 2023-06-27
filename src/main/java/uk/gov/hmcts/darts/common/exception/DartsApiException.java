package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@SuppressWarnings("PMD.NullAssignment")
public class DartsApiException extends RuntimeException {

    private final DartsApiError error;
    private final String detail;

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
        super(error.getTitle());

        this.error = error;
        this.detail = detail;
    }

    public DartsApiException(DartsApiError error, String detail, Throwable throwable) {
        super(error.getTitle(), throwable);

        this.error = error;
        this.detail = detail;
    }

}
