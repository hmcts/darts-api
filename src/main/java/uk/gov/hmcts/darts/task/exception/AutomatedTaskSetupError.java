package uk.gov.hmcts.darts.task.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.common.model.AutomatedTaskErrorCode;
import uk.gov.hmcts.darts.common.model.AutomatedTaskTitleErrors;

@Getter
@RequiredArgsConstructor
public enum AutomatedTaskSetupError implements DartsApiError {
    FAILED_TO_FIND_AUTOMATED_TASK(
        AutomatedTaskErrorCode.FAILED_TO_FIND_AUTOMATED_TASK.getValue(),
        null,
        AutomatedTaskTitleErrors.FAILED_TO_FIND_AUTOMATED_TASK.toString()
    ),
    INVALID_CRON_EXPRESSION(
        AutomatedTaskErrorCode.INVALID_CRON_EXPRESSION.getValue(),
        null,
        AutomatedTaskTitleErrors.INVALID_CRON_EXPRESSION.toString())
    ;

    private static final String ERROR_TYPE_PREFIX = "AUTOMATED_TASK";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

}
