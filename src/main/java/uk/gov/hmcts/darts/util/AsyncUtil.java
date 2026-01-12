package uk.gov.hmcts.darts.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.darts.task.config.AsyncTaskConfig;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
@SuppressWarnings("PMD.DoNotUseThreads")//Required for async processing
public final class AsyncUtil {
    private AsyncUtil() {
        // Empty constructor to prevent instantiation
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
        invokeAllAwaitTermination(tasks, config.isUseVirtualThreads(), config.getThreads(), config.getAsyncTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    public static void invokeAllAwaitTermination(List<Callable<Void>> tasks,
                                                 int threads, long timeout, TimeUnit timeUnit) throws InterruptedException {
        invokeAllAwaitTermination(tasks, false, threads, timeout, timeUnit);
    }

    public static void invokeAllAwaitTermination(List<Callable<Void>> tasks,
                                                 boolean useVirtualThreads,
                                                 int threads, long timeout, TimeUnit timeUnit) throws InterruptedException {
        log.info("Starting {} tasks with {} threads", useVirtualThreads ? "virtual" : tasks.size(), threads);
        SecurityContext callingThreadContext = SecurityContextHolder.getContext();

        Supplier<ExecutorService> executorServiceSupplier = () -> {
            if (useVirtualThreads) {
                return Executors.newVirtualThreadPerTaskExecutor();
            } else {
                return Executors.newFixedThreadPool(threads);
            }
        };

        try (ExecutorService rawPool = executorServiceSupplier.get();
             ExecutorService executor = new DelegatingSecurityContextExecutorService(rawPool, callingThreadContext)) {
            List<Future<Void>> futures;
            try {
                futures = executor.invokeAll(tasks, timeout, timeUnit);
            } finally {
                shutdownAndAwaitTermination(executor, timeout, timeUnit);
                executor.shutdownNow(); // ensure we don't keep threads alive after scheduled run
            }

            if (log.isInfoEnabled()) {
                logMetrics(futures);
            }
            log.info("All async tasks completed");
        }
    }

    private static void logMetrics(List<Future<Void>> futures) throws InterruptedException {
        int cancelled = 0;
        int failed = 0;

        for (Future<Void> f : futures) {
            if (f.isCancelled()) {
                cancelled++;
                continue;
            }
            try {
                f.get(0, TimeUnit.MILLISECONDS); // already done; just harvest exception
            } catch (ExecutionException e) {
                failed++;
                log.error("Async task failed", e.getCause());
            } catch (TimeoutException impossible) {
                // Shouldn't happen because invokeAll already returned.
                log.error("Unexpected timeout when harvesting async task result", impossible);
            }
        }

        if (cancelled > 0) {
            log.warn("{} tasks were cancelled (timeout likely hit)", cancelled);
        }
        if (failed > 0) {
            log.warn("{} tasks failed", failed);
        }

    }
}
