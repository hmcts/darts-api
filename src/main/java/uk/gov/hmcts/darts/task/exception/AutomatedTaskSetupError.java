package uk.gov.hmcts.darts.task.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum AutomatedTaskSetupError implements DartsApiError {
    FAILED_TO_FIND_AUTOMATED_TASK(
          "100",
          null,
          "Failed to find automated task"
    ),
    INVALID_CRON_EXPRESSION(
          "101",
          null,
          "Invalid cron expression"
    );

    private static final String ERROR_TYPE_PREFIX = "AUTOMATED_TASK";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
