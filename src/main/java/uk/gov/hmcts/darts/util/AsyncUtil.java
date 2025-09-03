package uk.gov.hmcts.darts.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.darts.task.config.AsyncTaskConfig;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressWarnings("PMD.DoNotUseThreads")//Required for async processing
public final class AsyncUtil {
    private AsyncUtil() {

    }

    public static void shutdownAndAwaitTermination(ExecutorService executorService, long timeout, TimeUnit unit) {
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
                                                 AsyncTaskConfig config) throws InterruptedException {
        invokeAllAwaitTermination(tasks, config.getThreads(), config.getAsyncTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    public static void invokeAllAwaitTermination(List<Callable<Void>> tasks,
                                                 int threads, long timeout, TimeUnit timeUnit) throws InterruptedException {
        log.info("Starting {} tasks with {} threads", tasks.size(), threads);
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //Add authentication to each task as auth is thread local
        List<Callable<Void>> tasksWithAuth = tasks.stream()
            .map(voidCallable -> (Callable<Void>) () -> {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return voidCallable.call();
            })
            .toList();

        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            executorService.invokeAll(tasksWithAuth, timeout, timeUnit);
            shutdownAndAwaitTermination(executorService, timeout, timeUnit);
        }
        log.info("All async tasks completed");
    }
}
