package uk.gov.hmcts.darts.task.helper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AutomatedTaskExceptionHelper {
    public boolean causedByInterruptedException(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof InterruptedException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
