package uk.gov.hmcts.darts.dailylist.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum DailyListError implements DartsApiError {

    FAILED_TO_PROCESS_DAILYLIST(
            "100",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to process daily list"
    );

    private static final String ERROR_TYPE_PREFIX = "DAILYLIST";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }
}
