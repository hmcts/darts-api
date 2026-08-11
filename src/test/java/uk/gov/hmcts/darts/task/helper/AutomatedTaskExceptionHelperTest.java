package uk.gov.hmcts.darts.task.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomatedTaskExceptionHelperTest {

    @Test
    void causedByInterruptedExceptionReturnsTrueWhenExceptionIsInterruptedException() {
        assertTrue(AutomatedTaskExceptionHelper.causedByInterruptedException(new InterruptedException()));
    }

    @Test
    void causedByInterruptedExceptionReturnsTrueWhenExceptionCauseIsInterruptedException() {
        RuntimeException exception = new RuntimeException(
            "Task failed",
            new IllegalStateException("Wrapped", new InterruptedException())
        );

        assertTrue(AutomatedTaskExceptionHelper.causedByInterruptedException(exception));
    }

    @Test
    void causedByInterruptedExceptionReturnsFalseWhenExceptionCauseIsNotInterruptedException() {
        RuntimeException exception = new RuntimeException(
            "Task failed",
            new IllegalStateException("Wrapped")
        );

        assertFalse(AutomatedTaskExceptionHelper.causedByInterruptedException(exception));
    }

    @Test
    void causedByInterruptedExceptionReturnsFalseWhenExceptionIsNull() {
        assertFalse(AutomatedTaskExceptionHelper.causedByInterruptedException(null));
    }
}
