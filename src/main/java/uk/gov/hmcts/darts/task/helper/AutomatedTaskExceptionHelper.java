package uk.gov.hmcts.darts.task.helper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AutomatedTaskExceptionHelper {
    public boolean causedByInterruptedException(Throwable exception) {
        while (exception != null) {
            if (exception instanceof InterruptedException) {
                return true;
            }
            exception = exception.getCause();
        }
        return false;
    }
}
