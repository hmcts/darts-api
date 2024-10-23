package uk.gov.hmcts.darts.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class AsyncUtil {
    private AsyncUtil() {

    }

    public static void shutdownAndAwaitTermination(ExecutorService executorService, int timeout, TimeUnit unit) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout, unit)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void invokeAllAwaitTermination(List<Callable<Void>> tasks,
                                                 int threads, int timeout, TimeUnit timeUnit) throws InterruptedException {
        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            executorService.invokeAll(tasks);
            shutdownAndAwaitTermination(executorService, timeout, timeUnit);
        }
    }
}
