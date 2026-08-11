package uk.gov.hmcts.darts.task.helper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AutomatedTaskExceptionHelper {
    public boolean causedByInterruptedException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof InterruptedException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
