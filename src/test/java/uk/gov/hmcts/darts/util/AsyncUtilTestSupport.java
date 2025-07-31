package uk.gov.hmcts.darts.util;

import lombok.SneakyThrows;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

public final class AsyncUtilTestSupport {

    private AsyncUtilTestSupport() {
    }

    /**
     * Bypasses async logic from AsyncUtil and run each provided callable synchronously.
     * Allowing Mockito to better Capture the tasks and verify their execution.
     **/
    @SneakyThrows
    public static void runSyncronously(Runnable runnable) {
        try (MockedStatic<AsyncUtil> mockedStatic = mockStatic(AsyncUtil.class)) {
            // Mock the static method call to throw InterruptedException

            // when
            runnable.run();
            //Should gracefully handle the exception

            ArgumentCaptor<List<Callable<Void>>> tasks = ArgumentCaptor.captor();
            mockedStatic.verify(() -> AsyncUtil.invokeAllAwaitTermination(tasks.capture(), any()));
            List<Callable<Void>> capturedTasks = tasks.getValue();
            if (capturedTasks.isEmpty()) {
                throw new IllegalStateException("No tasks were captured, expected at least one task to be run.");
            }
            for (Callable<Void> task : capturedTasks) {
                task.call();
            }
        }
    }
}
