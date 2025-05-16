package uk.gov.hmcts.darts.task.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskErrorCode;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskTitleErrors;

@Getter
@RequiredArgsConstructor
public enum AutomatedTaskApiError implements DartsApiError {

    AUTOMATED_TASK_NOT_FOUND(
        AutomatedTaskErrorCode.AUTOMATED_TASK_NOT_FOUND.getValue(),
        HttpStatus.NOT_FOUND,
        AutomatedTaskTitleErrors.AUTOMATED_TASK_NOT_FOUND.toString()
    ),
    AUTOMATED_TASK_ALREADY_RUNNING(
        AutomatedTaskErrorCode.AUTOMATED_TASK_ALREADY_RUNNING.getValue(),
        HttpStatus.CONFLICT,
        AutomatedTaskTitleErrors.AUTOMATED_TASK_ALREADY_RUNNING.toString(),
        false
    ),
    AUTOMATED_TASK_NOT_CONFIGURED_CORRECTLY(
        AutomatedTaskErrorCode.AUTOMATED_TASK_NOT_CONFIGURED.getValue(),
        HttpStatus.INTERNAL_SERVER_ERROR,
        AutomatedTaskTitleErrors.AUTOMATED_TASK_NOT_CONFIGURED.toString()
    ),
    INCORRECT_AUTOMATED_TASK_TYPE(
        AutomatedTaskErrorCode.INCORRECT_AUTOMATED_TASK_TYPE.getValue(),
        HttpStatus.UNPROCESSABLE_ENTITY,
        AutomatedTaskTitleErrors.INCORRECT_AUTOMATED_TASK_TYPE.toString()
    );

    private static final String ERROR_TYPE_PREFIX = "AUTOMATED_TASK";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;
    private final boolean shouldLogException;

    AutomatedTaskApiError(String errorTypeNumeric, HttpStatus httpStatus, String title) {
        this(errorTypeNumeric, httpStatus, title, true);
    }

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }

    @Override
    public boolean shouldLogException() {
        return shouldLogException;
    }
}
