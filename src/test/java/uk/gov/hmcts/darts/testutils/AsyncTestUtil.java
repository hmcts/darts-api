package uk.gov.hmcts.darts.testutils;

import org.mockito.MockedStatic;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.util.List;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockStatic;

public class AsyncTestUtil {

    public static void processTasksSynchronously(Runnable runnable) {
        try (MockedStatic<AsyncUtil> asyncUtilMock = mockStatic(AsyncUtil.class)) {
            //Mock AsyncUtil to synchronously execute the tasks so we can verify the batch processor calls within the test
            asyncUtilMock.when(() -> AsyncUtil.invokeAllAwaitTermination(anyList(), any()))
                .thenAnswer(invocation -> {
                    List<Callable<Void>> tasks = invocation.getArgument(0);
                    for (Callable<Void> task : tasks) {
                        task.call();
                    }
                    return null;
                });
            runnable.run();
        }
    }
}

